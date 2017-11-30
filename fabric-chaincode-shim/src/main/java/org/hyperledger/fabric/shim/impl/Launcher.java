/*
 * Copyright IBM Corp., DTCC All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.shim.impl;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

import com.google.common.base.Strings;

import java.io.File;

import javax.net.ssl.SSLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.Chaincode;

public class Launcher {

	private static Log logger = LogFactory.getLog(Launcher.class);

	private LauncherConfiguration configuration = new LauncherConfiguration();
	private Chaincode chaincode;

	public void start(Chaincode chaincode, String[] args) {
		// parse configuration
		this.configuration.load(args);
    this.chaincode = chaincode;
    final String ccId = configuration.getId();
    if (ccId == null) {
      throw new IllegalArgumentException("The chaincode id must be specified using either the -i "
					+ "or --i command line options or the CORE_CHAINCODE_ID_NAME environment variable.");
    } else {
      logger.trace("chaincode started");
      final ManagedChannel connection = newPeerClientConnection();
      logger.trace("connection created");
      chatWithPeer(connection, ccId);
    }
	}

	ManagedChannel newPeerClientConnection() {
		final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(configuration.getHost(), configuration.getPort());
		logger.info("Configuring channel connection to peer.");

		if (configuration.isTlsEnabled()) {
			logger.info("TLS is enabled");
			try {
				final SslContext sslContext = GrpcSslContexts.forClient().trustManager(new File(configuration.getRootCertFile())).build();
				builder.negotiationType(NegotiationType.TLS);
				String hostOverrideAuthority = configuration.getHostOverrideAuthority();
				if (!Strings.isNullOrEmpty(hostOverrideAuthority)) {
					logger.info("Host override " + hostOverrideAuthority);
					builder.overrideAuthority(hostOverrideAuthority);
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

	void chatWithPeer(ManagedChannel connection, String id) {
		ChatStream chatStream = newChatStream(connection);

		// Send the ChaincodeID during register.
		org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID chaincodeID = org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID.newBuilder()
				.setName(id)
				.build();

		ChaincodeShim.ChaincodeMessage payload = ChaincodeShim.ChaincodeMessage.newBuilder()
				.setPayload(chaincodeID.toByteString())
				.setType(ChaincodeShim.ChaincodeMessage.Type.REGISTER)
				.build();

		// Register on the stream
		logger.info(String.format("Registering as '%s' ... sending %s", id, ChaincodeShim.ChaincodeMessage.Type.REGISTER));
		chatStream.serialSend(payload);

		while (!Thread.currentThread().isInterrupted()) {
			try {
				chatStream.receive();
			} catch (Exception e) {
				logger.error("Receiving message error", e);
				break;
			}
		}
	}

	ChatStream newChatStream(ManagedChannel connection) {
		return new ChatStream(connection, chaincode);
	}

}
