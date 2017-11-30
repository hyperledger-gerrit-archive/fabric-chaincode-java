/*
 * Copyright IBM Corp., DTCC All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.shim.impl;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class LauncherConfigurationTest {

	@Mock
	Map<String, String> environment;

	@InjectMocks
	LauncherConfiguration configuration;

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Test
	public void load() throws Exception {
		configuration.load(new String[]{"-a", "1.1.1.1:1111", "-s", "-i", "ChaincodeId", "-o", "hostOverride"});
		assertEquals(configuration.getHost(), "1.1.1.1");
		assertEquals(configuration.getPort(), 1111);
		assertTrue(configuration.isTlsEnabled());
		assertEquals(configuration.getHostOverrideAuthority(), "hostOverride");
		assertEquals(configuration.getId(), "ChaincodeId");
		assertEquals(configuration.getRootCertFile(), "/etc/hyperledger/fabric/peer.crt");
	}

	@Test
	public void loadWithArgException() throws Exception {
		configuration.load(new String[]{"-a", "1.1.1.1:errorPort", "-s", "-i", "ChaincodeId", "-o", "hostOverride"});
		assertEquals(configuration.getHost(), LauncherConfiguration.DEFAULT_HOST);
		assertEquals(configuration.getPort(), LauncherConfiguration.DEFAULT_PORT);
		assertFalse(configuration.isTlsEnabled());
		assertEquals(configuration.getHostOverrideAuthority(), "");
		assertEquals(configuration.getId(), null);
		assertEquals(configuration.getRootCertFile(), "/etc/hyperledger/fabric/peer.crt");
	}

	@Test
	public void loadWithSystemEnv() throws Exception {
		when(environment.containsKey(anyString())).thenReturn(true);
		doReturn("ChaincodeId").when(environment).get(LauncherConfiguration.CORE_CHAINCODE_ID_NAME);
		doReturn("1.1.1.1:1111").when(environment).get(LauncherConfiguration.CORE_PEER_ADDRESS);
		doReturn("true").when(environment).get(LauncherConfiguration.CORE_PEER_TLS_ENABLED);
		doReturn("hostOverride").when(environment).get(LauncherConfiguration.CORE_PEER_TLS_SERVERHOSTOVERRIDE);
		doReturn("certPath").when(environment).get(LauncherConfiguration.CORE_PEER_TLS_ROOTCERT_FILE);
		configuration.load(new String[0]);
		assertEquals(configuration.getHost(), "1.1.1.1");
		assertEquals(configuration.getPort(), 1111);
		assertTrue(configuration.isTlsEnabled());
		assertEquals(configuration.getHostOverrideAuthority(), "hostOverride");
		assertEquals(configuration.getId(), "ChaincodeId");
		assertEquals(configuration.getRootCertFile(), "certPath");
	}
}