/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim.mock.peer;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc;
import org.hyperledger.fabric.shim.utils.EnvUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ChaincodeMockPeer {
    private static final Logger logger = Logger.getLogger(ChaincodeMockPeer.class.getName());

    private final int port;
    private final Server server;
    private final List<ScenarioStep> scenario;
    private final ChaincodeMockPeerService service;

    public ChaincodeMockPeer(List<ScenarioStep> scenario, int port) throws IOException {
        this.port = port;
        this.scenario = scenario;
        this.service = new ChaincodeMockPeerService(this.scenario);
        ServerBuilder<?> sb = ServerBuilder.forPort(port);
        this.server = sb.addService(this.service).build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                ChaincodeMockPeer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    public void send(ChaincodeShim.ChaincodeMessage msg) {
        this.service.lastMessageSend = msg;
        System.err.println("Mock peer => Sending message: " + msg);
        this.service.observer.onNext(msg);
    }

    public int getStartedStep() {
       return this.service.startStepNumber;
    }

    public int getEndedStep() {
        return this.service.endStepNumber;
    }

    public ChaincodeShim.ChaincodeMessage getLastMessageRcvd() {
        return this.service.lastMessageRcvd;
    }

    public ChaincodeShim.ChaincodeMessage getLastMessageSend() {
        return this.service.lastMessageSend;
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static ChaincodeMockPeer startServer(List<ScenarioStep> scenario) throws Exception{
        Map<String, String> env = new HashMap<>();
        env.put("CORE_CHAINCODE_LOGGING_SHIM", "INFO");
        env.put("CORE_CHAINCODE_LOGGING_LEVEL", "INFO");
        EnvUtil.setEnv(env);

        ChaincodeMockPeer server = new ChaincodeMockPeer(scenario,7052);
        server.start();
        return server;
    }


    private static class ChaincodeMockPeerService extends ChaincodeSupportGrpc.ChaincodeSupportImplBase {
        final List<ScenarioStep> scenario;
        int startStepNumber;
        int endStepNumber;
        ChaincodeShim.ChaincodeMessage lastMessageRcvd;
        ChaincodeShim.ChaincodeMessage lastMessageSend;
        StreamObserver<ChaincodeShim.ChaincodeMessage> observer;


        public ChaincodeMockPeerService(List<ScenarioStep> scenario) {
            this.scenario = scenario;
            this.startStepNumber = 0;
            this.endStepNumber = 0;
        }

        @Override
        public StreamObserver<ChaincodeShim.ChaincodeMessage> register(final StreamObserver<ChaincodeShim.ChaincodeMessage> responseObserver) {
            observer = responseObserver;
            return new StreamObserver<ChaincodeShim.ChaincodeMessage>() {

                @Override
                public void onNext(ChaincodeShim.ChaincodeMessage chaincodeMessage) {
                    System.err.println("Mock peer => Got message: " + chaincodeMessage);
                    ChaincodeMockPeerService.this.lastMessageRcvd = chaincodeMessage;
                    if (ChaincodeMockPeerService.this.scenario.size() > 0) {
                        ScenarioStep step = ChaincodeMockPeerService.this.scenario.get(0);
                        ChaincodeMockPeerService.this.scenario.remove(0);
                        ChaincodeMockPeerService.this.startStepNumber++;
                        if (step.expected(chaincodeMessage)) {
                            List<ChaincodeShim.ChaincodeMessage> nextSteps = step.next();
                            for (ChaincodeShim.ChaincodeMessage m : nextSteps) {
                                if (step.waitPeriod() != 0) {
                                    try {
                                        Thread.sleep(step.waitPeriod());
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                ChaincodeMockPeerService.this.lastMessageSend = m;
                                System.err.println("Mock peer => Sending response message: " + m);
                                responseObserver.onNext(m);
                            }
                        }
                        ChaincodeMockPeerService.this.endStepNumber++;
                    }
                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            };
        }
    }

}
