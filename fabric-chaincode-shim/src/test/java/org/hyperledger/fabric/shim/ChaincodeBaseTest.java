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
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;

import static org.hamcrest.Matchers.is;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.*;
import static org.junit.Assert.*;

public class ChaincodeBaseTest {

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
    public void newSuccessResponseEmpty() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newSuccessResponse();
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.SUCCESS);
        assertNull("Response message in not null", response.getMessage());
        assertNull("Response payload in not null", response.getPayload());
    }

    @Test
    public void newSuccessResponseWithMessage() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newSuccessResponse("Simple message");
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.SUCCESS);
        assertEquals("Response message in not correct", "Simple message", response.getMessage());
        assertNull("Response payload in not null", response.getPayload());
    }

    @Test
    public void newSuccessResponseWithPayload() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newSuccessResponse("Simple payload".getBytes(Charset.defaultCharset()));
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.SUCCESS);
        assertNull("Response message in not null", response.getMessage());
        assertArrayEquals("Response payload in not null", response.getPayload(), "Simple payload".getBytes(Charset.defaultCharset()));
    }

    @Test
    public void newSuccessResponseWithMessageAndPayload() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newSuccessResponse("Simple message", "Simple payload".getBytes(Charset.defaultCharset()));
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.SUCCESS);
        assertEquals("Response message in not correct", "Simple message", response.getMessage());
        assertArrayEquals("Response payload in not null", response.getPayload(), "Simple payload".getBytes(Charset.defaultCharset()));
    }

    @Test
    public void newErrorResponseEmpty() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newErrorResponse();
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR);
        assertNull("Response message in not null", response.getMessage());
        assertNull("Response payload in not null", response.getPayload());
    }

    @Test
    public void newErrorResponseWithMessage() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newErrorResponse("Simple message");
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR);
        assertEquals("Response message in not correct", "Simple message", response.getMessage());
        assertNull("Response payload in not null", response.getPayload());
    }

    @Test
    public void newErrorResponseWithPayload() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newErrorResponse("Simple payload".getBytes(Charset.defaultCharset()));
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR);
        assertNull("Response message in not null", response.getMessage());
        assertArrayEquals("Response payload in not null", response.getPayload(), "Simple payload".getBytes(Charset.defaultCharset()));
    }

    @Test
    public void newErrorResponseWithMessageAndPayload() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newErrorResponse("Simple message", "Simple payload".getBytes(Charset.defaultCharset()));
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR);
        assertEquals("Response message in not correct", "Simple message", response.getMessage());
        assertArrayEquals("Response payload in not null", response.getPayload(), "Simple payload".getBytes(Charset.defaultCharset()));
    }

    @Test
    public void newErrorResponseWithException() {
        org.hyperledger.fabric.shim.Chaincode.Response response = ChaincodeBase.newErrorResponse(new Exception("Simple exception"));
        assertEquals("Response status is incorrect", response.getStatus(), org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR);
        assertEquals("Response message in not correct", "Simple exception", response.getMessage());
        assertNotNull("Response payload in null", response.getPayload());
    }

    @Test
    public void testLogLevelMapping() {
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
        assertEquals("Wrong mapping", Level.SEVERE, cb.mapLevel("CRITICAL"));
        assertEquals("Wrong mapping", Level.SEVERE, cb.mapLevel("ERROR"));
        assertEquals("Wrong mapping", Level.WARNING, cb.mapLevel("WARNING"));
        assertEquals("Wrong mapping", Level.INFO, cb.mapLevel("INFO"));
        assertEquals("Wrong mapping", Level.CONFIG, cb.mapLevel("NOTICE"));
        assertEquals("Wrong mapping", Level.FINEST, cb.mapLevel("DEBUG"));
        assertEquals("Wrong mapping", Level.INFO, cb.mapLevel("BLAH_BLAH"));
    }

    @Test
    public void testOptions() throws Exception{
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

        assertEquals("Host incorrect", "127.0.0.1", cb.getHost());
        assertEquals("Port incorrect", 7051, cb.getPort());
        assertFalse("TLS should not be enabled", cb.isTlsEnabled());

        Map<String, String> env = new HashMap<>();
        env.put("CORE_CHAINCODE_LOGGING_SHIM", "INFO");
        env.put("CORE_CHAINCODE_LOGGING_LEVEL", "INFO");
        env.put("CORE_CHAINCODE_ID_NAME", "mycc");
        env.put("CORE_PEER_ADDRESS", "localhost:7052");
        env.put("CORE_PEER_TLS_ENABLED", "true");
        env.put("CORE_PEER_TLS_ROOTCERT_FILE", "non_exist_path1");
        env.put("CORE_TLS_CLIENT_KEY_PATH", "non_exist_path2");
        env.put("CORE_TLS_CLIENT_CERT_PATH", "non_exist_path3");
        setEnv(env);
        cb.processEnvironmentOptions();
        assertEquals("CCId incorrect", cb.getId(), "mycc");
        assertEquals("Host incorrect", cb.getHost(), "localhost");
        assertEquals("Port incorrect", cb.getPort(), 7052);
        assertTrue("TLS should be enabled", cb.isTlsEnabled());
        assertEquals("Root certificate file", "non_exist_path1", cb.getTlsClientRootCertPath());
        assertEquals("Client key file", "non_exist_path2", cb.getTlsClientKeyPath());
        assertEquals("Client certificate file", "non_exist_path3", cb.getTlsClientCertPath());

        env.put("CORE_PEER_ADDRESS", "localhost1");
        setEnv(env);
        cb.processEnvironmentOptions();
        assertEquals("Host incorrect", cb.getHost(), "localhost");
        assertEquals("Port incorrect", cb.getPort(), 7052);

        try {
            cb.validateOptions();
        } catch (IllegalArgumentException e) {fail("Wrong arguments"); }

        cb.processCommandLineOptions(new String[] {"-i", "mycc1", "--peerAddress", "localhost.org:7053"});
        assertEquals("CCId incorrect", cb.getId(), "mycc1");
        assertEquals("Host incorrect", cb.getHost(), "localhost.org");
        assertEquals("Port incorrect", cb.getPort(), 7053);

        try {
            cb.validateOptions();
        } catch (IllegalArgumentException e) {fail("Wrong arguments"); }

        cb.processCommandLineOptions(new String[] {"-i", "mycc1", "--peerAddress", "localhost1.org.7054"});
        assertEquals("Host incorrect", cb.getHost(), "localhost.org");
        assertEquals("Port incorrect", cb.getPort(), 7053);

        env.put("CORE_PEER_TLS_ENABLED", "false");
    }

    @Test
    public void newChannelBuilderTest() throws Exception{
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

        Map<String, String> env = new HashMap<>();
        env.put("CORE_CHAINCODE_LOGGING_SHIM", "INFO");
        env.put("CORE_CHAINCODE_LOGGING_LEVEL", "INFO");
        env.put("CORE_CHAINCODE_ID_NAME", "mycc");
        env.put("CORE_PEER_ADDRESS", "localhost:7052");
        env.put("CORE_PEER_TLS_ENABLED", "true");
        env.put("CORE_PEER_TLS_ROOTCERT_FILE", "src/test/resources/ca.crt");
        env.put("CORE_TLS_CLIENT_KEY_PATH", "src/test/resources/client.key.enc");
        env.put("CORE_TLS_CLIENT_CERT_PATH", "src/test/resources/client.crt.enc");
        setEnv(env);

        cb.processEnvironmentOptions();
        cb.validateOptions();
        cb.newChannelBuilder();
    }
}
