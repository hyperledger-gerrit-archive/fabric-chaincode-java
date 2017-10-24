/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.impl.ChatStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ChaincodeBaseTest {

	@Mock
	ManagedChannel managedChannel;

	@Mock
	ChatStream chatStream;

	@Mock
	ClientCall clientCall;

	@Spy
	ChaincodeBase chaincodeBase;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Test
	public void start() throws Exception {
		doReturn(managedChannel).when(chaincodeBase).newPeerClientConnection();
		doNothing().when(chaincodeBase).chatWithPeer(managedChannel);
		chaincodeBase.start(new String[]{"-a", "1.1.1.1:1111", "-s", "-i", "ccId", "ChaincodeId", "-o", "hostOverride"});
		Thread.sleep(0);
		assertThat(chaincodeBase, hasProperty("host", equalTo("1.1.1.1")));
		assertThat(chaincodeBase, hasProperty("port", equalTo(1111)));
		assertThat(chaincodeBase, hasProperty("tlsEnabled", equalTo(true)));
		assertThat(chaincodeBase, hasProperty("hostOverrideAuthority", equalTo("hostOverride")));
		assertThat(chaincodeBase, hasProperty("id", equalTo("ccId")));
		assertThat(chaincodeBase, hasProperty("rootCertFile", equalTo("/etc/hyperledger/fabric/peer.crt")));
		verify(chaincodeBase).newPeerClientConnection();
		verify(chaincodeBase).chatWithPeer(managedChannel);
	}

	@Test
	public void startWithArgException() throws Exception {
		doReturn(managedChannel).when(chaincodeBase).newPeerClientConnection();
		doNothing().when(chaincodeBase).chatWithPeer(managedChannel);
		chaincodeBase.start(new String[]{"-a", "1.1.1.1:errorPort", "-s", "-i", "ccId", "ChaincodeId", "-o", "hostOverride"});
		Thread.sleep(0);
		assertThat(chaincodeBase, hasProperty("host", equalTo(ChaincodeBase.DEFAULT_HOST)));
		assertThat(chaincodeBase, hasProperty("port", equalTo(ChaincodeBase.DEFAULT_PORT)));
		assertThat(chaincodeBase, hasProperty("tlsEnabled", equalTo(false)));
		assertThat(chaincodeBase, hasProperty("hostOverrideAuthority", equalTo("")));
		assertThat(chaincodeBase, hasProperty("id", equalTo(null)));
		assertThat(chaincodeBase, hasProperty("rootCertFile", equalTo("/etc/hyperledger/fabric/peer.crt")));
		verify(chaincodeBase).newPeerClientConnection();
		verify(chaincodeBase).chatWithPeer(managedChannel);
	}

	@Test
	public void startWithSystemEnv() throws Exception {
		when(chaincodeBase.envContainsKey(anyString())).thenReturn(true);
		doReturn("ccId").when(chaincodeBase).envGet("CORE_CHAINCODE_ID_NAME");
		doReturn("1.1.1.1:1111").when(chaincodeBase).envGet("CORE_PEER_ADDRESS");
		doReturn("true").when(chaincodeBase).envGet("CORE_PEER_TLS_ENABLED");
		doReturn("hostOverride").when(chaincodeBase).envGet("CORE_PEER_TLS_SERVERHOSTOVERRIDE");
		doReturn("certPath").when(chaincodeBase).envGet("CORE_PEER_TLS_ROOTCERT_FILE");

		doReturn(managedChannel).when(chaincodeBase).newPeerClientConnection();
		doNothing().when(chaincodeBase).chatWithPeer(managedChannel);
		chaincodeBase.start(new String[0]);
		Thread.sleep(0);
		assertThat(chaincodeBase, hasProperty("host", equalTo("1.1.1.1")));
		assertThat(chaincodeBase, hasProperty("port", equalTo(1111)));
		assertThat(chaincodeBase, hasProperty("tlsEnabled", equalTo(true)));
		assertThat(chaincodeBase, hasProperty("hostOverrideAuthority", equalTo("hostOverride")));
		assertThat(chaincodeBase, hasProperty("id", equalTo("ccId")));
		assertThat(chaincodeBase, hasProperty("rootCertFile", equalTo("certPath")));
		verify(chaincodeBase).newPeerClientConnection();
		verify(chaincodeBase).chatWithPeer(managedChannel);
	}

	@Test
	public void newPeerClientConnectionWithTLSDisabled() throws Exception {
		doReturn(false).when(chaincodeBase).isTlsEnabled();
		doReturn("1.1.1.1").when(chaincodeBase).getHost();
		doReturn(1111).when(chaincodeBase).getPort();

		assertNotNull(chaincodeBase.newPeerClientConnection());
	}

	@Test
	public void newPeerClientConnection() throws Exception {
		doReturn(true).when(chaincodeBase).isTlsEnabled();
		doReturn("1.1.1.1").when(chaincodeBase).getHost();
		doReturn(1111).when(chaincodeBase).getPort();
		doReturn("hostOverride").when(chaincodeBase).getHostOverrideAuthority();
		doReturn("cert").when(chaincodeBase).getRootCertFile();

		File cert = new File("cert");
		cert.deleteOnExit();
		try (FileOutputStream out = new FileOutputStream(cert)) {
			out.write(getX509Certificate());
			out.flush();
			out.close();

			assertNotNull(chaincodeBase.newPeerClientConnection());
		} finally {
			cert.deleteOnExit();
		}
	}

	private byte[] getX509Certificate() throws Exception {
		return ("-----BEGIN CERTIFICATE-----\n" +
				"MIIDlzCCAn+gAwIBAgIJAMgs1d9bT93vMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNV\n" +
				"BAYTAktSMQ4wDAYDVQQIDAVTZW91bDEOMAwGA1UEBwwFU2VvdWwxDTALBgNVBAoM\n" +
				"BEtPTkExDDAKBgNVBAsMA0NPUzEWMBQGA1UEAwwNd3d3LmtvbmFpLmNvbTAeFw0x\n" +
				"NzEwMjMwNzEzNDdaFw0yMjA2MTkwNzEzNDdaMGIxCzAJBgNVBAYTAktSMQ4wDAYD\n" +
				"VQQIDAVTZW91bDEOMAwGA1UEBwwFU2VvdWwxDTALBgNVBAoMBEtPTkExDDAKBgNV\n" +
				"BAsMA0NPUzEWMBQGA1UEAwwNd3d3LmtvbmFpLmNvbTCCASIwDQYJKoZIhvcNAQEB\n" +
				"BQADggEPADCCAQoCggEBALVPJjzcaN5lfDBNq3VAeCoooRBG7hpN2rm84LxtXqLb\n" +
				"5upN6noCN66gp2L8G8CAXhTpj1Pm2U7iAheuPA3WPOexLFO62VPjxnGQ6JcpcOPb\n" +
				"TCvVCsODFtA7W9iMwqSuMSO2S0gPQCm48cXrOMRgAy8YrasHIXTDb76SdeijTDrc\n" +
				"+hbY+EGK+WlEbp3psBibr/8+2M1IGhViBSW8XcJYioBEGVcKFSJhDt1cJBZtkrSp\n" +
				"ukdlnxh51mV0dwNGEW96LwtYIaXjBBacRWVph31RTMQhwUz8vTLhgqDvhLPC0byL\n" +
				"FvZ/cOSsUgWVAV82yFNz1exkyAnhubfjsqR4/X4yOXUCAwEAAaNQME4wHQYDVR0O\n" +
				"BBYEFM/MslzxfUGrg0my7XJiKzVjCmcCMB8GA1UdIwQYMBaAFM/MslzxfUGrg0my\n" +
				"7XJiKzVjCmcCMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBACAHNJCA\n" +
				"QT/a9AC18Z7xqMRei+ZKYEMqTzlkTE7BB0lNgVAWV46gOU7jD+qvSVwM+jAMtcYN\n" +
				"3UoUN4amGHooSnGDOJT8VOTq9B437s1JxIWcmRGa9Lh1MbhwsdcW7+TQi/93UN8C\n" +
				"pJpwYVvycPG9JvS7FMF1P9VQLxFWV0bQBS+m+qG80hOfL7YG8b0x9vyO0TiAyred\n" +
				"tbVnBcmfAnJFj9UxHoMzTLEcWJDKwC5RzLNxXbnraSfDOJw44TgS198HOx77aBRz\n" +
				"HYJ5ACL6C0lyH0FIFwPSXtrvosTJ9UCGBaowBmL9R480FVZsJR4fp65KVolBx8t0\n" +
				"W7w49uYfsQxE1rU=\n" +
				"-----END CERTIFICATE-----").getBytes();
	}

	@Test
	public void chatWithPeer() throws Exception {
		doReturn(chatStream).when(chaincodeBase).newChatSteam(managedChannel);
		doReturn("123").when(chaincodeBase).getId();
		doNothing().doThrow(new RuntimeException()).when(chatStream).receive();
		chaincodeBase.chatWithPeer(managedChannel);

		verify(chatStream).serialSend(argThat(message -> {
			try {
				assertEquals("123", org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID.newBuilder().mergeFrom(message.getPayload()).build().getName());
			} catch (InvalidProtocolBufferException e) {
				assertNull(e);
			}
			assertEquals(ChaincodeShim.ChaincodeMessage.Type.REGISTER, message.getType());
			return true;
		}));
		verify(chatStream, times(2)).receive();
	}

	@Test
	public void testNewChatSteam() throws Exception {
		when(managedChannel.newCall(any(), any())).thenReturn(clientCall);
		chaincodeBase.newChatSteam(managedChannel);
		verify(managedChannel).newCall(any(), any());
	}

	@Test
	public void newSuccessResponseNoArgs() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse();
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newSuccessResponseWithMessageOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse("message");
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newSuccessResponseWithPayloadOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse("payload".getBytes());
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", equalTo("payload".getBytes())));
	}

	@Test
	public void newSuccessResponse() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse("message", "payload".getBytes());
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", equalTo("payload".getBytes())));
	}

	@Test
	public void newErrorResponseWithException() throws Exception {
		RuntimeException exception = new RuntimeException("message");
		StringWriter stack = new StringWriter();
		Chaincode.Response response = ChaincodeBase.newErrorResponse(exception);
		exception.printStackTrace(new PrintWriter(stack));
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", equalTo(stack.toString().getBytes())));
	}

	@Test
	public void newErrorResponseNoArgs() throws Exception {
		Chaincode.Response response = ChaincodeBase.newErrorResponse();
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newErrorResponseWithMessageOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newErrorResponse("message");
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newErrorResponseWithPayloadOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newErrorResponse("payload".getBytes());
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", equalTo("payload".getBytes())));
	}
}