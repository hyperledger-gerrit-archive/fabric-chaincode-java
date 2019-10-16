/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.fabric.Logging;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.shim.impl.ChaincodeSupportStream;
import org.hyperledger.fabric.shim.impl.Handler;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;

public abstract class ChaincodeBase implements Chaincode {

    public static final String CORE_CHAINCODE_LOGGING_SHIM = "CORE_CHAINCODE_LOGGING_SHIM";
    public static final String CORE_CHAINCODE_LOGGING_LEVEL = "CORE_CHAINCODE_LOGGING_LEVEL";

    @Override
    public abstract Response init(ChaincodeStub stub);

    @Override
    public abstract Response invoke(ChaincodeStub stub);

    private static final Logger logger = Logger.getLogger(ChaincodeBase.class.getName());

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 7051;

    private String host = DEFAULT_HOST;
    private int port = DEFAULT_PORT;
    private boolean tlsEnabled = false;
    private String tlsClientKeyPath;
    private String tlsClientCertPath;
    private String tlsClientRootCertPath;

    private String id;

    private static final String CORE_CHAINCODE_ID_NAME = "CORE_CHAINCODE_ID_NAME";
    private static final String CORE_PEER_ADDRESS = "CORE_PEER_ADDRESS";
    private static final String CORE_PEER_TLS_ENABLED = "CORE_PEER_TLS_ENABLED";
    private static final String CORE_PEER_TLS_ROOTCERT_FILE = "CORE_PEER_TLS_ROOTCERT_FILE";
    private static final String ENV_TLS_CLIENT_KEY_PATH = "CORE_TLS_CLIENT_KEY_PATH";
    private static final String ENV_TLS_CLIENT_CERT_PATH = "CORE_TLS_CLIENT_CERT_PATH";
    private Level logLevel;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Start chaincode
     *
     * @param args command line arguments
     */
    public void start(final String[] args) {
        try {
            processEnvironmentOptions();
            processCommandLineOptions(args);
            initializeLogging();
            validateOptions();
            connectToPeer();
        } catch (final Exception e) {
            logger.severe("Chaincode could not start");
            logger.severe(Logging.formatError(e));
            throw new RuntimeException(e);
        }
    }

    protected void connectToPeer() throws IOException {
        final ChaincodeID chaincodeId = ChaincodeID.newBuilder().setName(this.id).build();
        final ManagedChannelBuilder<?> channelBuilder = newChannelBuilder();
        final Handler handler = new Handler(chaincodeId, this);
        new ChaincodeSupportStream(channelBuilder, handler::onChaincodeMessage, handler::nextOutboundChaincodeMessage);
    }

    protected void initializeLogging() {
        
        final LogManager logManager = LogManager.getLogManager();

        final Formatter f = new Formatter() {

            private final Date dat = new Date();
            private final String format = "%1$tH:%1$tM:%1$tS:%1$tL %4$-7.7s %2$-80.80s %5$s%6$s%n";

            @Override
            public String format(final LogRecord record) {
                dat.setTime(record.getMillis());
                String source;
                if (record.getSourceClassName() != null) {
                    source = record.getSourceClassName();
                    if (record.getSourceMethodName() != null) {
                        source += " " + record.getSourceMethodName();
                    }
                } else {
                    source = record.getLoggerName();
                }
                final String message = formatMessage(record);
                String throwable = "";
                if (record.getThrown() != null) {
                    final StringWriter sw = new StringWriter();
                    final PrintWriter pw = new PrintWriter(sw);
                    pw.println();
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    throwable = sw.toString();
                }
                return String.format(format, dat, source, record.getLoggerName(), record.getLevel(), message,
                        throwable);

            }

        };

        final Logger rootLogger = Logger.getLogger("org.hyperledger");
        rootLogger.setUseParentHandlers(false);
        rootLogger.addHandler(new ConsoleHandler());

        final ArrayList<String> allLoggers = Collections.list(logManager.getLoggerNames());
        allLoggers.add("org.hyperledger");
        allLoggers.stream().filter(name -> name.startsWith("org.hyperledger")).peek(System.out::println)
                .map(name -> logManager.getLogger(name)).forEach(logger -> {
                    logger.setLevel(this.logLevel);
                    for (final java.util.logging.Handler handler : logger.getHandlers()) {
                        handler.setLevel(Level.ALL);
                        handler.setFormatter(f);
                    }
                });

        rootLogger.info("Loglevel set to " + this.logLevel);
    }

    private Level mapLevel(final String level) {
        if (level != null) {
            switch (level.toUpperCase().trim()) {
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
            }
        }
        return Level.INFO;
    }

    protected void validateOptions() {
        if (this.id == null) {
            throw new IllegalArgumentException(format(
                    "The chaincode id must be specified using either the -i or --i command line options or the %s environment variable.",
                    CORE_CHAINCODE_ID_NAME));
        }
        if (this.tlsEnabled) {
            if (tlsClientCertPath == null) {
                throw new IllegalArgumentException(
                        format("Client key certificate chain (%s) was not specified.", ENV_TLS_CLIENT_CERT_PATH));
            }
            if (tlsClientKeyPath == null) {
                throw new IllegalArgumentException(
                        format("Client key (%s) was not specified.", ENV_TLS_CLIENT_KEY_PATH));
            }
            if (tlsClientRootCertPath == null) {
                throw new IllegalArgumentException(
                        format("Peer certificate trust store (%s) was not specified.", CORE_PEER_TLS_ROOTCERT_FILE));
            }
        }
    }

    protected void processCommandLineOptions(final String[] args) {
        final Options options = new Options();
        options.addOption("a", "peer.address", true, "Address of peer to connect to");
        options.addOption(null, "peerAddress", true, "Address of peer to connect to");
        options.addOption("i", "id", true, "Identity of chaincode");

        try {
            final CommandLine cl = new DefaultParser().parse(options, args);
            if (cl.hasOption("peerAddress") || cl.hasOption('a')) {
                String hostAddrStr;
                if (cl.hasOption('a')) {
                    hostAddrStr = cl.getOptionValue('a');
                } else {
                    hostAddrStr = cl.getOptionValue("peerAddress");
                }
                final String[] hostArr = hostAddrStr.split(":");
                if (hostArr.length == 2) {
                    port = Integer.valueOf(hostArr[1].trim());
                    host = hostArr[0].trim();
                } else {
                    final String msg = String.format(
                            "peer address argument should be in host:port format, current %s in wrong", hostAddrStr);
                    logger.severe(msg);
                    throw new IllegalArgumentException(msg);
                }
            }
            if (cl.hasOption('i')) {
                id = cl.getOptionValue('i');
            }
        } catch (final Exception e) {
            logger.warning(() -> "cli parsing failed with exception" + Logging.formatError(e));
        }

        logger.info("<<<<<<<<<<<<<CommandLine options>>>>>>>>>>>>");
        logger.info("CORE_CHAINCODE_ID_NAME: " + this.id);
        logger.info("CORE_PEER_ADDRESS: " + this.host + ":" + this.port);
        logger.info("CORE_PEER_TLS_ENABLED: " + this.tlsEnabled);
        logger.info("CORE_PEER_TLS_ROOTCERT_FILE: " + this.tlsClientRootCertPath);
        logger.info("CORE_TLS_CLIENT_KEY_PATH: " + this.tlsClientKeyPath);
        logger.info("CORE_TLS_CLIENT_CERT_PATH: " + this.tlsClientCertPath);
    }

    protected void processEnvironmentOptions() {

        if (System.getenv().containsKey(CORE_CHAINCODE_ID_NAME)) {
            this.id = System.getenv(CORE_CHAINCODE_ID_NAME);
        }
        if (System.getenv().containsKey(CORE_PEER_ADDRESS)) {
            final String[] hostArr = System.getenv(CORE_PEER_ADDRESS).split(":");
            if (hostArr.length == 2) {
                this.port = Integer.valueOf(hostArr[1].trim());
                this.host = hostArr[0].trim();
            } else {
                final String msg = String.format(
                        "peer address argument should be in host:port format, ignoring current %s",
                        System.getenv(CORE_PEER_ADDRESS));
                logger.severe(msg);
            }
        }
        this.tlsEnabled = Boolean.parseBoolean(System.getenv(CORE_PEER_TLS_ENABLED));
        if (this.tlsEnabled) {
            this.tlsClientRootCertPath = System.getenv(CORE_PEER_TLS_ROOTCERT_FILE);
            this.tlsClientKeyPath = System.getenv(ENV_TLS_CLIENT_KEY_PATH);
            this.tlsClientCertPath = System.getenv(ENV_TLS_CLIENT_CERT_PATH);
        }

        Level chaincodeLogLevel = mapLevel(System.getenv(CORE_CHAINCODE_LOGGING_LEVEL));
        final Level shimLogLevel = mapLevel(System.getenv(CORE_CHAINCODE_LOGGING_SHIM));
        if (chaincodeLogLevel.intValue() > shimLogLevel.intValue()) {
            chaincodeLogLevel = shimLogLevel;
        }

        this.logLevel = chaincodeLogLevel;

        logger.info("<<<<<<<<<<<<<Enviromental options>>>>>>>>>>>>");
        logger.info("CORE_CHAINCODE_ID_NAME: " + this.id);
        logger.info("CORE_PEER_ADDRESS: " + this.host);
        logger.info("CORE_PEER_TLS_ENABLED: " + this.tlsEnabled);
        logger.info("CORE_PEER_TLS_ROOTCERT_FILE: " + this.tlsClientRootCertPath);
        logger.info("CORE_TLS_CLIENT_KEY_PATH: " + this.tlsClientKeyPath);
        logger.info("CORE_TLS_CLIENT_CERT_PATH: " + this.tlsClientCertPath);

        logger.info("LOGLEVEL: " + this.logLevel);
    }

    ManagedChannelBuilder<?> newChannelBuilder() throws IOException {
        final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port);
        logger.info("()->Configuring channel connection to peer." + host + ":" + port + " tlsenabled " + tlsEnabled);

        if (tlsEnabled) {
            builder.negotiationType(NegotiationType.TLS);
            builder.sslContext(createSSLContext());
        } else {
            builder.usePlaintext(true);
        }
        return builder;
    }

    SslContext createSSLContext() throws IOException {
        final byte[] ckb = Files.readAllBytes(Paths.get(this.tlsClientKeyPath));
        final byte[] ccb = Files.readAllBytes(Paths.get(this.tlsClientCertPath));

        return GrpcSslContexts.forClient().trustManager(new File(this.tlsClientRootCertPath))
                .keyManager(new ByteArrayInputStream(Base64.getDecoder().decode(ccb)),
                        new ByteArrayInputStream(Base64.getDecoder().decode(ckb)))
                .build();
    }

    @Deprecated
    protected static Response newSuccessResponse(final String message, final byte[] payload) {
        return ResponseUtils.newSuccessResponse(message, payload);
    }

    @Deprecated
    protected static Response newSuccessResponse() {
        return ResponseUtils.newSuccessResponse();
    }

    @Deprecated
    protected static Response newSuccessResponse(final String message) {
        return ResponseUtils.newSuccessResponse(message);
    }

    @Deprecated
    protected static Response newSuccessResponse(final byte[] payload) {
        return ResponseUtils.newSuccessResponse(payload);
    }

    @Deprecated
    protected static Response newErrorResponse(final String message, final byte[] payload) {
        return ResponseUtils.newErrorResponse(message, payload);
    }

    @Deprecated
    protected static Response newErrorResponse() {
        return ResponseUtils.newErrorResponse();
    }

    @Deprecated
    protected static Response newErrorResponse(final String message) {
        return ResponseUtils.newErrorResponse(message);
    }

    @Deprecated
    protected static Response newErrorResponse(final byte[] payload) {
        return ResponseUtils.newErrorResponse(payload);
    }

    @Deprecated
    protected static Response newErrorResponse(final Throwable throwable) {
        return ResponseUtils.newErrorResponse(throwable);
    }

    String getHost() {
        return host;
    }

    int getPort() {
        return port;
    }

    boolean isTlsEnabled() {
        return tlsEnabled;
    }

    String getTlsClientKeyPath() {
        return tlsClientKeyPath;
    }

    String getTlsClientCertPath() {
        return tlsClientCertPath;
    }

    String getTlsClientRootCertPath() {
        return tlsClientRootCertPath;
    }

    String getId() {
        return id;
    }
}
