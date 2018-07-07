/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim;

import org.hyperledger.fabric.shim.utils.EnvUtil;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.junit.Assert.*;

public class ChaincodeBaseTest {
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

        assertEquals("Host incorrect", ChaincodeBase.DEFAULT_HOST, cb.getHost());
        assertEquals("Port incorrect", ChaincodeBase.DEFAULT_PORT, cb.getPort());
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
        EnvUtil.setEnv(env);
        cb.processEnvironmentOptions();
        assertEquals("CCId incorrect", cb.getId(), "mycc");
        assertEquals("Host incorrect", cb.getHost(), "localhost");
        assertEquals("Port incorrect", cb.getPort(), 7052);
        assertTrue("TLS should be enabled", cb.isTlsEnabled());
        assertEquals("Root certificate file", "non_exist_path1", cb.getTlsClientRootCertPath());
        assertEquals("Client key file", "non_exist_path2", cb.getTlsClientKeyPath());
        assertEquals("Client certificate file", "non_exist_path3", cb.getTlsClientCertPath());

        env.put("CORE_PEER_ADDRESS", "localhost1");
        EnvUtil.setEnv(env);
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
        EnvUtil.setEnv(env);

        cb.processEnvironmentOptions();
        cb.validateOptions();
        cb.newChannelBuilder();
    }
}
