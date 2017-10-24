/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type;
import org.hyperledger.fabric.shim.impl.ChatStream;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR;
import static org.hyperledger.fabric.shim.Chaincode.Response.Status.SUCCESS;

public abstract class ChaincodeBase implements Chaincode {

	@Override
	public abstract Response init(ChaincodeStub stub);

	@Override
	public abstract Response invoke(ChaincodeStub stub);

	private static Log logger = LogFactory.getLog(ChaincodeBase.class);

	public static final String DEFAULT_HOST = "127.0.0.1";
	public static final int DEFAULT_PORT = 7051;

	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;
	private String hostOverrideAuthority = "";
	private boolean tlsEnabled = false;
	private String rootCertFile = "/etc/hyperledger/fabric/peer.crt";

	private String id;

	private final static String CORE_CHAINCODE_ID_NAME = "CORE_CHAINCODE_ID_NAME";
	private final static String CORE_PEER_ADDRESS = "CORE_PEER_ADDRESS";
	private final static String CORE_PEER_TLS_ENABLED = "CORE_PEER_TLS_ENABLED";
	private final static String CORE_PEER_TLS_SERVERHOSTOVERRIDE = "CORE_PEER_TLS_SERVERHOSTOVERRIDE";
	private static final String CORE_PEER_TLS_ROOTCERT_FILE = "CORE_PEER_TLS_ROOTCERT_FILE";

	/**
	 * Start chaincode
	 *
	 * @param args command line arguments
	 */
	public void start(String[] args) {
		processEnvironmentOptions();
		processCommandLineOptions(args);
		if (this.id == null) {
			logger.error(String.format("The chaincode id must be specified using either the -i or --i command line options or the %s environment variable.", CORE_CHAINCODE_ID_NAME));
		}
		new Thread(() -> {
			logger.trace("chaincode started");
			final ManagedChannel connection = newPeerClientConnection();
			logger.trace("connection created");
			chatWithPeer(connection);
			logger.trace("chatWithPeer DONE");
		}).start();
	}

	private void processCommandLineOptions(String[] args) {
		Options options = new Options();
		options.addOption("a", "peerAddress", true, "Address of peer to connect to");
		options.addOption("s", "securityEnabled", false, "Present if security is enabled");
		options.addOption("i", "id", true, "Identity of chaincode");
		options.addOption("o", "hostNameOverride", true, "Hostname override for server certificate");
		try {
			CommandLine cl = new DefaultParser().parse(options, args);
			if (cl.hasOption('a')) {
				setPeerAddress(cl.getOptionValue('a'));
			}
			if (cl.hasOption('s')) {
				tlsEnabled = true;
				logger.info("TLS enabled");
				if (cl.hasOption('o')) {
					hostOverrideAuthority = cl.getOptionValue('o');
					logger.info("server host override given " + hostOverrideAuthority);
				}
			}
			if (cl.hasOption('i')) {
				id = cl.getOptionValue('i');
			}
		} catch (Exception e) {
			logger.warn("cli parsing failed with exception", e);

		}
	}

	private void setPeerAddress(String peerAddress) {
		String[] tokens = peerAddress.split(":");
		port = new Integer(tokens[1]);
		host = tokens[0];
	}

	private void processEnvironmentOptions() {
		if (envContainsKey(CORE_CHAINCODE_ID_NAME)) {
			this.id = envGet(CORE_CHAINCODE_ID_NAME);
		}
		if (envContainsKey(CORE_PEER_ADDRESS)) {
			setPeerAddress(envGet(CORE_PEER_ADDRESS));
		}
		if (envContainsKey(CORE_PEER_TLS_ENABLED)) {
			this.tlsEnabled = Boolean.parseBoolean(envGet(CORE_PEER_TLS_ENABLED));
			if (envContainsKey(CORE_PEER_TLS_SERVERHOSTOVERRIDE)) {
				this.hostOverrideAuthority = envGet(CORE_PEER_TLS_SERVERHOSTOVERRIDE);
			}
			if (envContainsKey(CORE_PEER_TLS_ROOTCERT_FILE)) {
				this.rootCertFile = envGet(CORE_PEER_TLS_ROOTCERT_FILE);
			}
		}
	}

	boolean envContainsKey(String key) {
		return System.getenv().containsKey(key);
	}

	String envGet(String key) {
		return System.getenv(key);
	}

	ManagedChannel newPeerClientConnection() {
		final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(getHost(), getPort());
		logger.info("Configuring channel connection to peer.");

		if (isTlsEnabled()) {
			logger.info("TLS is enabled");
			try {
				final SslContext sslContext = GrpcSslContexts.forClient().trustManager(new File(getRootCertFile())).build();
				builder.negotiationType(NegotiationType.TLS);
				if (!"".equals(getHostOverrideAuthority())) {
					logger.info("Host override " + getHostOverrideAuthority());
					builder.overrideAuthority(getHostOverrideAuthority());
				}
				builder.sslContext(sslContext);
				logger.info("TLS context built: " + sslContext);
			} catch (SSLException e) {
				logger.error("failed connect to peer with SSLException", e);
			}
		} else {
			builder.usePlaintext(true);
		}
		return builder.build();
	}

	void chatWithPeer(ManagedChannel connection) {
		ChatStream chatStream = newChatSteam(connection);

		// Send the ChaincodeID during register.
		String id = getId();
		ChaincodeID chaincodeID = ChaincodeID.newBuilder()
				.setName(id)
				.build();

		ChaincodeMessage payload = ChaincodeMessage.newBuilder()
				.setPayload(chaincodeID.toByteString())
				.setType(Type.REGISTER)
				.build();

		// Register on the stream
		logger.info(String.format("Registering as '%s' ... sending %s", id, Type.REGISTER));
		chatStream.serialSend(payload);

		while (true) {
			try {
				chatStream.receive();
			} catch (Exception e) {
				logger.error("Receiving message error", e);
				break;
			}
		}
	}

	ChatStream newChatSteam(ManagedChannel connection) {
		return new ChatStream(connection, this);
	}

	String getHost() {
		return host;
	}

	int getPort() {
		return port;
	}

	String getHostOverrideAuthority() {
		return hostOverrideAuthority;
	}

	boolean isTlsEnabled() {
		return tlsEnabled;
	}

	String getId() {
		return id;
	}

	String getRootCertFile() {
		return rootCertFile;
	}

	protected static Response newSuccessResponse(String message, byte[] payload) {
		return new Response(SUCCESS, message, payload);
	}

	protected static Response newSuccessResponse() {
		return newSuccessResponse(null, null);
	}

	protected static Response newSuccessResponse(String message) {
		return newSuccessResponse(message, null);
	}

	protected static Response newSuccessResponse(byte[] payload) {
		return newSuccessResponse(null, payload);
	}

	protected static Response newErrorResponse(String message, byte[] payload) {
		return new Response(INTERNAL_SERVER_ERROR, message, payload);
	}

	protected static Response newErrorResponse() {
		return newErrorResponse(null, null);
	}

	protected static Response newErrorResponse(String message) {
		return newErrorResponse(message, null);
	}

	protected static Response newErrorResponse(byte[] payload) {
		return newErrorResponse(null, payload);
	}

	protected static Response newErrorResponse(Throwable throwable) {
		return newErrorResponse(throwable.getMessage(), printStackTrace(throwable));
	}

	private static byte[] printStackTrace(Throwable throwable) {
		if (throwable == null) return null;
		final StringWriter buffer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(buffer));
		return buffer.toString().getBytes(StandardCharsets.UTF_8);
	}
}
