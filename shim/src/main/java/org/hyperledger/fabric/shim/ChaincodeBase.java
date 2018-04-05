/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim;

import static java.lang.String.format;
import static java.util.logging.Level.ALL;
import static org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR;
import static org.hyperledger.fabric.shim.Chaincode.Response.Status.SUCCESS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.shim.impl.ChaincodeSupportStream;
import org.hyperledger.fabric.shim.impl.Handler;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

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
	private boolean tlsEnabled = false;
	private String tlsClientKeyPath;
	private String tlsClientCertPath;
	private String tlsClientRootCertPath;

	private String id;

	private final static String CORE_CHAINCODE_ID_NAME = "CORE_CHAINCODE_ID_NAME";
	private final static String CORE_PEER_ADDRESS = "CORE_PEER_ADDRESS";
	private final static String CORE_PEER_TLS_ENABLED = "CORE_PEER_TLS_ENABLED";
	private static final String CORE_PEER_TLS_ROOTCERT_FILE = "CORE_PEER_TLS_ROOTCERT_FILE";
	private static final String ENV_TLS_CLIENT_KEY_PATH = "CORE_TLS_CLIENT_KEY_PATH";
	private static final String ENV_TLS_CLIENT_CERT_PATH = "CORE_TLS_CLIENT_CERT_PATH";

	/**
	 * Start chaincode
	 *
	 * @param args command line arguments
	 */
	public void start(String[] args) {
		processEnvironmentOptions();
		processCommandLineOptions(args);
		initializeLogging();
		try {
			validateOptions();
			final ChaincodeID chaincodeId = ChaincodeID.newBuilder().setName(this.id).build();
			final Chaincode chaincode = (Chaincode)this;
			final ManagedChannelBuilder<?> channelBuilder = newChannelBuilder();
			final Handler handler = new Handler(chaincodeId, chaincode);
			new ChaincodeSupportStream(channelBuilder, handler::onChaincodeMessage, handler::nextOutboundChaincodeMessage);
		} catch (IllegalArgumentException e) {
			logger.fatal("Chaincode could not start", e);
		}
	}

	private void initializeLogging() {
		System.setProperty("java.util.logging.SimpleFormatter.format","%1$tH:%1$tM:%1$tS:%1$tL %4$-7.7s %2$s %5$s%6$s%n");
		final Logger rootLogger = Logger.getLogger("");
		for(java.util.logging.Handler handler: rootLogger.getHandlers()) {
			handler.setLevel(ALL);
			handler.setFormatter(new SimpleFormatter() {
				@Override
				public synchronized String format(LogRecord record) {
					return super.format(record)
							.replaceFirst(".*SEVERE\\s*\\S*\\s*\\S*", "\u001B[1;31m$0\u001B[0m")
							.replaceFirst(".*WARNING\\s*\\S*\\s*\\S*", "\u001B[1;33m$0\u001B[0m")
							.replaceFirst(".*CONFIG\\s*\\S*\\s*\\S*", "\u001B[35m$0\u001B[0m")
							.replaceFirst(".*FINE\\s*\\S*\\s*\\S*", "\u001B[36m$0\u001B[0m")
							.replaceFirst(".*FINER\\s*\\S*\\s*\\S*", "\u001B[36m$0\u001B[0m")
							.replaceFirst(".*FINEST\\s*\\S*\\s*\\S*", "\u001B[36m$0\u001B[0m");
				}
			});
		}
		// set logging level of shim logger
		Logger.getLogger("org.hyperledger.fabric.shim").setLevel(mapLevel(System.getenv("CORE_CHAINCODE_LOGGING_SHIM")));

		// set logging level of chaincode logger
		Logger.getLogger(this.getClass().getPackage().getName()).setLevel(mapLevel(System.getenv("CORE_CHAINCODE_LOGGING_LEVEL")));

	}

	private Level mapLevel(String level) {
		switch(level) {
		case "CRITICAL":
		case "ERROR":
			return Level.SEVERE;
		case "WARNING":
			return Level.WARNING;
		case "INFO":
			return Level.INFO;
		case "NOTICE":
			return Level.CONFIG;
		case "DEBUG":
			return Level.FINEST;
		default:
			return Level.INFO;
		}
	}

	private void validateOptions() {
		if (this.id == null) {
			throw new IllegalArgumentException(format("The chaincode id must be specified using either the -i or --i command line options or the %s environment variable.", CORE_CHAINCODE_ID_NAME));
		}
		if (this.tlsEnabled) {
			if (tlsClientCertPath == null) {
				throw new IllegalArgumentException(format("Client key certificate chain (%s) was not specified.", ENV_TLS_CLIENT_CERT_PATH));
			}
			if (tlsClientKeyPath == null) {
				throw new IllegalArgumentException(format("Client key (%s) was not specified.", ENV_TLS_CLIENT_KEY_PATH));
			}
			if (tlsClientRootCertPath == null) {
				throw new IllegalArgumentException(format("Peer certificate trust store (%s) was not specified.", CORE_PEER_TLS_ROOTCERT_FILE));
			}
		}
	}

	private void processCommandLineOptions(String[] args) {
		Options options = new Options();
		options.addOption("a", "peer.address", true, "Address of peer to connect to");
		options.addOption(null, "peerAddress", true, "Address of peer to connect to");
		options.addOption("i", "id", true, "Identity of chaincode");
		try {
			CommandLine cl = new DefaultParser().parse(options, args);
			if (cl.hasOption("peerAddress") || cl.hasOption('a')) {
				if (cl.hasOption('a')) {
					host = cl.getOptionValue('a');
				} else {
					host = cl.getOptionValue("peerAddress");
				}
				port = Integer.valueOf(host.split(":")[1]);
				host = host.split(":")[0];
			}
			if (cl.hasOption('i')) {
				id = cl.getOptionValue('i');
			}
		} catch (Exception e) {
			logger.warn("cli parsing failed with exception", e);

		}
	}

	private void processEnvironmentOptions() {
		if (System.getenv().containsKey(CORE_CHAINCODE_ID_NAME)) {
			this.id = System.getenv(CORE_CHAINCODE_ID_NAME);
		}
		if (System.getenv().containsKey(CORE_PEER_ADDRESS)) {
			this.host = System.getenv(CORE_PEER_ADDRESS);
		}
		this.tlsEnabled = Boolean.parseBoolean(System.getenv(CORE_PEER_TLS_ENABLED));
		if(this.tlsEnabled) {
			this.tlsClientRootCertPath = System.getenv(CORE_PEER_TLS_ROOTCERT_FILE);
			this.tlsClientKeyPath = System.getenv(ENV_TLS_CLIENT_KEY_PATH);
			this.tlsClientCertPath = System.getenv(ENV_TLS_CLIENT_CERT_PATH);
		}
	}

	private ManagedChannelBuilder<?> newChannelBuilder() {
		final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port);
		logger.info("Configuring channel connection to peer.");

		if (tlsEnabled) {
			logger.info("TLS is enabled");
			try {
				final SslContext sslContext = GrpcSslContexts.forClient()
						.trustManager(new File(this.tlsClientRootCertPath))
						.keyManager(
								new ByteArrayInputStream(Base64.getDecoder().decode(Files.readAllBytes(Paths.get(this.tlsClientCertPath)))),
								new ByteArrayInputStream(Base64.getDecoder().decode(Files.readAllBytes(Paths.get(this.tlsClientKeyPath)))))
						.build();
				builder.negotiationType(NegotiationType.TLS);
				builder.sslContext(sslContext);
				logger.info("TLS context built: " + sslContext);
			} catch (IOException e) {
				logger.fatal("failed connect to peer", e);
			}
		} else {
			builder.usePlaintext(true);
		}
		return builder;
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
