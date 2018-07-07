/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.shim.impl.ChaincodeSupportStream;
import org.hyperledger.fabric.shim.impl.Handler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.lang.String.format;
import static java.util.logging.Level.ALL;
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

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Start chaincode
     *
     * @param args command line arguments
     */
    public void start(String[] args) {
        try {
            processEnvironmentOptions();
            processCommandLineOptions(args);
            initializeLogging();
            validateOptions();
            final ChaincodeID chaincodeId = ChaincodeID.newBuilder().setName(this.id).build();
            final ManagedChannelBuilder<?> channelBuilder = newChannelBuilder();
            final Handler handler = new Handler(chaincodeId, this);
            new ChaincodeSupportStream(channelBuilder, handler::onChaincodeMessage, handler::nextOutboundChaincodeMessage);

        } catch (IllegalArgumentException e) {
            logger.fatal("Chaincode could not start", e);
        }
    }

    private void initializeLogging() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tH:%1$tM:%1$tS:%1$tL %4$-7.7s %2$s %5$s%6$s%n");
        final Logger rootLogger = Logger.getLogger("");
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
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

    Level mapLevel(String level) {
        switch (level) {
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

    void validateOptions() {
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

    void processCommandLineOptions(String[] args) {
        Options options = new Options();
        options.addOption("a", "peer.address", true, "Address of peer to connect to");
        options.addOption(null, "peerAddress", true, "Address of peer to connect to");
        options.addOption("i", "id", true, "Identity of chaincode");

        try {
            CommandLine cl = new DefaultParser().parse(options, args);
            if (cl.hasOption("peerAddress") || cl.hasOption('a')) {
                String hostAddrStr;
                if (cl.hasOption('a')) {
                    hostAddrStr = cl.getOptionValue('a');
                } else {
                    hostAddrStr = cl.getOptionValue("peerAddress");
                }
                String[] hostArr = hostAddrStr.split(":");
                if (hostArr.length == 2) {
                    port = Integer.valueOf(hostArr[1].trim());
                    host = hostArr[0].trim();
                } else {
                    String msg = String.format("peer address argument should be in host:port format, current %s in wrong", hostAddrStr);
                    logger.error(msg);
                    throw new IllegalArgumentException(msg);
                }
            }
            if (cl.hasOption('i')) {
                id = cl.getOptionValue('i');
            }
        } catch (Exception e) {
            logger.warn("cli parsing failed with exception", e);
        }

        logger.info("<<<<<<<<<<<<<CommandLine options>>>>>>>>>>>>");
        logger.info("CORE_CHAINCODE_ID_NAME: " + this.id);
        logger.info("CORE_PEER_ADDRESS: " + this.host + ":" + this.port);
        logger.info("CORE_PEER_TLS_ENABLED: " + this.tlsEnabled);
        logger.info("CORE_PEER_TLS_ROOTCERT_FILE" + this.tlsClientRootCertPath);
        logger.info("CORE_TLS_CLIENT_KEY_PATH" + this.tlsClientKeyPath);
        logger.info("CORE_TLS_CLIENT_CERT_PATH" + this.tlsClientCertPath);
    }

    void processEnvironmentOptions() {
        if (System.getenv().containsKey(CORE_CHAINCODE_ID_NAME)) {
            this.id = System.getenv(CORE_CHAINCODE_ID_NAME);
        }
        if (System.getenv().containsKey(CORE_PEER_ADDRESS)) {
            String[] hostArr = System.getenv(CORE_PEER_ADDRESS).split(":");
            if (hostArr.length == 2) {
                this.port = Integer.valueOf(hostArr[1].trim());
                this.host = hostArr[0].trim();
            } else {
                String msg = String.format("peer address argument should be in host:port format, ignoring current %s", System.getenv(CORE_PEER_ADDRESS));
                logger.error(msg);
            }
        }
        this.tlsEnabled = Boolean.parseBoolean(System.getenv(CORE_PEER_TLS_ENABLED));
        if (this.tlsEnabled) {
            this.tlsClientRootCertPath = System.getenv(CORE_PEER_TLS_ROOTCERT_FILE);
            this.tlsClientKeyPath = System.getenv(ENV_TLS_CLIENT_KEY_PATH);
            this.tlsClientCertPath = System.getenv(ENV_TLS_CLIENT_CERT_PATH);
        }

        logger.info("<<<<<<<<<<<<<Enviromental options>>>>>>>>>>>>");
        logger.info("CORE_CHAINCODE_ID_NAME: " + this.id);
        logger.info("CORE_PEER_ADDRESS: " + this.host);
        logger.info("CORE_PEER_TLS_ENABLED: " + this.tlsEnabled);
        logger.info("CORE_PEER_TLS_ROOTCERT_FILE" + this.tlsClientRootCertPath);
        logger.info("CORE_TLS_CLIENT_KEY_PATH" + this.tlsClientKeyPath);
        logger.info("CORE_TLS_CLIENT_CERT_PATH" + this.tlsClientCertPath);
    }

    ManagedChannelBuilder<?> newChannelBuilder() {
        final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port);
        logger.info("Configuring channel connection to peer.");

        if (tlsEnabled) {
            try {
                builder.negotiationType(NegotiationType.TLS);
                builder.sslContext(createSSLContext());
            } catch (IOException e) {
                logger.fatal("failed connect to peer", e);
            }
        } else {
            builder.usePlaintext(true);
        }
        return builder;
    }

    SslContext createSSLContext() throws IOException{
        byte ckb[] = Files.readAllBytes(Paths.get(this.tlsClientKeyPath));
        byte ccb[] = Files.readAllBytes(Paths.get(this.tlsClientCertPath));

        return GrpcSslContexts.forClient()
                .trustManager(new File(this.tlsClientRootCertPath))
                .keyManager(
                        new ByteArrayInputStream(Base64.getDecoder().decode(ccb)),
                        new ByteArrayInputStream(Base64.getDecoder().decode(ckb)))
                .build();
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

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public String getTlsClientKeyPath() {
        return tlsClientKeyPath;
    }

    public String getTlsClientCertPath() {
        return tlsClientCertPath;
    }

    public String getTlsClientRootCertPath() {
        return tlsClientRootCertPath;
    }

    public String getId() {
        return id;
    }


}
