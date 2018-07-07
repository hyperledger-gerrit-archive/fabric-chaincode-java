/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.mock.peer.*;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.hamcrest.Matchers.is;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.*;
import static org.junit.Assert.assertThat;

public class ChaincodeBaseTest {

    ChaincodeMockPeer server;

    @After
    public void afterTest() throws Exception{
        server.stop();
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

        server = startServer(scenario);

        cb.start(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});

        Thread.sleep(1000);

        assertThat(server.getStartedStep(), is(1));
        assertThat(server.getEndedStep(), is(1));

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

        ByteString payload = Chaincode.ChaincodeInput.newBuilder().addArgs(ByteString.copyFromUtf8("")).build().toByteString();
        ChaincodeShim.ChaincodeMessage initMsg = MessageUtil.newEventMessage(INIT, "testChannel", "0", payload, null);

        List<ScenarioStep> scenario = new ArrayList<>();
        scenario.add(new RegisterStep());
        scenario.add(new CompleteStep());

        server = startServer(scenario);

        cb.start(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});

        Thread.sleep(1000);

        assertThat(server.getStartedStep(), is(1));
        assertThat(server.getEndedStep(), is(1));

        server.send(initMsg);
        Thread.sleep(1000);

        assertThat(server.getStartedStep(), is(2));
        assertThat(server.getEndedStep(), is(2));

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

        server = startServer(scenario);

        cb.start(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});

        Thread.sleep(1000);

        assertThat(server.getStartedStep(), is(1));
        assertThat(server.getEndedStep(), is(1));

        server.send(initMsg);
        Thread.sleep(1000);

        assertThat(server.getStartedStep(), is(4));
        assertThat(server.getEndedStep(), is(4));

        assertThat(server.getLastMessageSend().getType(), is(RESPONSE));
        assertThat(server.getLastMessageRcvd().getType(), is(COMPLETED));
    }

    protected static ChaincodeMockPeer startServer(List<ScenarioStep> scenario) throws Exception{
        Map<String, String> env = new HashMap<>();
        env.put("CORE_CHAINCODE_LOGGING_SHIM", "INFO");
        env.put("CORE_CHAINCODE_LOGGING_LEVEL", "INFO");
        setEnv(env);

        ChaincodeMockPeer server = new ChaincodeMockPeer(scenario,7052);
        server.start();
        return server;
    }

    protected static void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }

    @Test
    @Ignore
    public void newSuccessResponse() {

    }

    @Test
    @Ignore
    public void newSuccessResponse1() {
    }

    @Test
    @Ignore
    public void newSuccessResponse2() {
    }

    @Test
    @Ignore
    public void newSuccessResponse3() {
    }

    @Test
    @Ignore
    public void newErrorResponse() {
    }

    @Test
    @Ignore
    public void newErrorResponse1() {
    }

    @Test
    @Ignore
    public void newErrorResponse2() {
    }

    @Test
    @Ignore
    public void newErrorResponse3() {
    }

    @Test
    @Ignore
    public void newErrorResponse4() {
    }
}
