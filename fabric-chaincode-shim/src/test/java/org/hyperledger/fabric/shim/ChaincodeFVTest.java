/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.mock.peer.*;
import org.hyperledger.fabric.shim.utils.EnvUtil;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.is;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ChaincodeFVTest {

    ChaincodeMockPeer server;

    @After
    public void afterTest() throws Exception{
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Test
    public void TestRegister() throws Exception {
        ChaincodeBase cb = new ChaincodeBase() {
            @Override
            public Response init(ChaincodeStub stub) {
                return newSuccessResponse();
            }

            @Override
            public Response invoke(ChaincodeStub stub) {
                return newSuccessResponse();
            }
        };


        List<ScenarioStep> scenario = new ArrayList<>();
        scenario.add(new RegisterStep());

        server = ChaincodeMockPeer.startServer(scenario);

        cb.start(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});

        checkScenarioStepEnded(server, 1, 1000, TimeUnit.MILLISECONDS);

        assertThat(server.getLastMessageSend().getType(), is(READY));
        assertThat(server.getLastMessageRcvd().getType(), is(REGISTER));
    }

    @Test
    public void TestRegisterAndEmptyInit() throws Exception {
        ChaincodeBase cb = new ChaincodeBase() {
            @Override
            public Response init(ChaincodeStub stub) {
                return newSuccessResponse();
            }

            @Override
            public Response invoke(ChaincodeStub stub) {
                return newSuccessResponse();
            }
        };

        ByteString payload = org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInput.newBuilder().addArgs(ByteString.copyFromUtf8("")).build().toByteString();
        ChaincodeShim.ChaincodeMessage initMsg = MessageUtil.newEventMessage(INIT, "testChannel", "0", payload, null);

        List<ScenarioStep> scenario = new ArrayList<>();
        scenario.add(new RegisterStep());
        scenario.add(new CompleteStep());

        server = ChaincodeMockPeer.startServer(scenario);

        cb.start(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});
        checkScenarioStepEnded(server, 1, 1000, TimeUnit.MILLISECONDS);

        server.send(initMsg);
        checkScenarioStepEnded(server, 2, 1000, TimeUnit.MILLISECONDS);

        assertThat(server.getLastMessageSend().getType(), is(INIT));
        assertThat(server.getLastMessageRcvd().getType(), is(COMPLETED));
    }

    @Test
    public void TestRegisterAndInitWithArguments() throws Exception {
        ChaincodeBase cb = new ChaincodeBase() {
            @Override
            public Response init(ChaincodeStub stub) {
                assertThat(stub.getFunction(), is("init"));
                assertThat(stub.getArgs().size(), is(5));
                stub.putState("a", ByteString.copyFromUtf8("100").toByteArray());
                stub.putState("b", ByteString.copyFromUtf8("200").toByteArray());
                return newSuccessResponse();
            }

            @Override
            public Response invoke(ChaincodeStub stub) {
                return newSuccessResponse();
            }
        };

        ByteString payload = Chaincode.ChaincodeInput.newBuilder()
                .addArgs(ByteString.copyFromUtf8("init"))
                .addArgs(ByteString.copyFromUtf8("a"))
                .addArgs(ByteString.copyFromUtf8("100"))
                .addArgs(ByteString.copyFromUtf8("b"))
                .addArgs(ByteString.copyFromUtf8("200"))
                .build().toByteString();
        ChaincodeShim.ChaincodeMessage initMsg = MessageUtil.newEventMessage(INIT, "testChannel", "0", payload, null);

        List<ScenarioStep> scenario = new ArrayList<>();
        scenario.add(new RegisterStep());
        scenario.add(new PutValueState());
        scenario.add(new PutValueState());
        scenario.add(new CompleteStep());

        server = ChaincodeMockPeer.startServer(scenario);

        cb.start(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});
        checkScenarioStepEnded(server, 1, 1000, TimeUnit.MILLISECONDS);

        server.send(initMsg);
        checkScenarioStepEnded(server, 4, 1000, TimeUnit.MILLISECONDS);

        assertThat(server.getLastMessageSend().getType(), is(RESPONSE));
        assertThat(server.getLastMessageRcvd().getType(), is(COMPLETED));
    }

    @Test
    public void TestStreamShutdown() throws Exception {
        ChaincodeBase cb = new ChaincodeBase() {
            @Override
            public Response init(ChaincodeStub stub) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
                return newSuccessResponse();
            }

            @Override
            public Response invoke(ChaincodeStub stub) {
                return newSuccessResponse();
            }
        };

        ByteString payload = org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInput.newBuilder().addArgs(ByteString.copyFromUtf8("")).build().toByteString();
        ChaincodeShim.ChaincodeMessage initMsg = MessageUtil.newEventMessage(INIT, "testChannel", "0", payload, null);

        List<ScenarioStep> scenario = new ArrayList<>();
        scenario.add(new RegisterStep());
        scenario.add(new CompleteStep());

        server = ChaincodeMockPeer.startServer(scenario);

        cb.start(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});
        checkScenarioStepEnded(server, 1, 1000, TimeUnit.MILLISECONDS);
        server.send(initMsg);
        server.stop();
        server = null;
    }

    public static void checkScenarioStepEnded(final ChaincodeMockPeer s, final int step, final int timeout, final TimeUnit units) throws Exception {
        try {
            EnvUtil.runWithTimeout(new Thread(() -> {
                while (true) {
                    if (s.getEndedStep() == step) return;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
            }), timeout, units);
        } catch (TimeoutException e) {
            fail("Got timeout, first step not finished");
        }

    }
}
