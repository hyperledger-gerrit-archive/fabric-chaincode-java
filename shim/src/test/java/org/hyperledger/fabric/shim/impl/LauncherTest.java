/*
 * Copyright IBM Corp., DTCC All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.shim.impl;

import io.grpc.ClientCall;
import io.grpc.ManagedChannel;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.FileOutputStream;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LauncherTest {

	@Spy
	@InjectMocks
	Launcher launcher;

	@Mock
	ManagedChannel managedChannel;

	@Mock
	ChatStream chatStream;

	@Mock
	ClientCall clientCall;

	@Mock
	LauncherConfiguration launcherConfiguration;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Test
	public void start() throws Exception {
		final String chaincodeId = "ChaincodeId";
		final String[] args = new String[]{"arg1", "arg2"};
		doReturn(managedChannel).when(launcher).newPeerClientConnection();
		doNothing().when(launcher).chatWithPeer(eq(managedChannel), eq(chaincodeId));
		when(launcherConfiguration.getId()).thenReturn(chaincodeId);
		launcher.start(TestChaincodeImpl.class, args);
		Thread.sleep(100);
		verify(launcher).newPeerClientConnection();
		verify(launcher).chatWithPeer(eq(managedChannel), eq(chaincodeId));
		verify(launcherConfiguration).load(args);
	}

	public static class TestChaincodeImpl extends ChaincodeBase {
		@Override
		public Response init(ChaincodeStub stub) {
			return null; //Do nothing
		}

		@Override
		public Response invoke(ChaincodeStub stub) {
			return null; //Do nothing
		}
	}

	@Test(expected = RuntimeException.class)
	public void startWithErrorChaincodeClass() throws Exception {
		launcher.start(Chaincode.class, new String[]{"arg1", "arg2"});
	}

	@Test
	public void newPeerClientConnectionWithTLSDisabled() throws Exception {
		when(launcherConfiguration.isTlsEnabled()).thenReturn(false);
		when(launcherConfiguration.getHost()).thenReturn("1.1.1.1");
		when(launcherConfiguration.getPort()).thenReturn(1111);

		assertNotNull(launcher.newPeerClientConnection());
	}

	@Test
	public void newPeerClientConnection() throws Exception {
		when(launcherConfiguration.isTlsEnabled()).thenReturn(true);
		when(launcherConfiguration.getHost()).thenReturn("1.1.1.1");
		when(launcherConfiguration.getPort()).thenReturn(1111);
		when(launcherConfiguration.getHostOverrideAuthority()).thenReturn("hostOverride");
		when(launcherConfiguration.getRootCertFile()).thenReturn("cert");

		File cert = new File("cert");
		cert.deleteOnExit();
		try (FileOutputStream out = new FileOutputStream(cert)) {
			out.write(getX509Certificate());
			out.flush();
			out.close();

			assertNotNull(launcher.newPeerClientConnection());
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
		final String chaincodeId = "123";
		doReturn(chatStream).when(launcher).newChatStream(managedChannel);
		doNothing().doThrow(new RuntimeException()).when(chatStream).receive();
		launcher.chatWithPeer(managedChannel, chaincodeId);

		verify(chatStream).serialSend(argThat(message -> {
			try {
				assertEquals(chaincodeId, org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID.newBuilder().mergeFrom(message.getPayload()).build().getName());
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
		launcher.newChatStream(managedChannel);
		verify(managedChannel).newCall(any(), any());
	}
}