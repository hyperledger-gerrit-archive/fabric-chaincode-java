/*
 * Copyright IBM Corp., DTCC All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.shim.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class LauncherConfigurationTest {

	@Spy
	Map<String, String> environment = new HashMap<>();

	@InjectMocks
	LauncherConfiguration configuration;

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Test
	public void load() throws Exception {
		configuration.load(new String[]{"-a", "1.1.1.1:1111", "-s", "-i", "ChaincodeId", "-o", "hostOverride"});
		assertEquals("1.1.1.1", configuration.getHost());
		assertEquals(1111, configuration.getPort());
		assertTrue(configuration.isTlsEnabled());
		assertEquals("hostOverride", configuration.getHostOverrideAuthority());
		assertEquals("ChaincodeId", configuration.getId());
		assertEquals("/etc/hyperledger/fabric/peer.crt", configuration.getRootCertFile());
	}

	@Test
	public void loadWithArgException() throws Exception {
		configuration.load(new String[]{"-a", "1.1.1.1:errorPort", "-s", "-i", "ChaincodeId", "-o", "hostOverride"});
		assertEquals(LauncherConfiguration.DEFAULT_HOST, configuration.getHost());
		assertEquals(LauncherConfiguration.DEFAULT_PORT, configuration.getPort());
		assertFalse(configuration.isTlsEnabled());
		assertEquals("", configuration.getHostOverrideAuthority());
		assertNull(configuration.getId());
		assertEquals("/etc/hyperledger/fabric/peer.crt", configuration.getRootCertFile());
	}

	@Test
	public void loadWithSystemEnv() throws Exception {
		when(environment.containsKey(anyString())).thenReturn(true);
		environment.put(LauncherConfiguration.CORE_CHAINCODE_ID_NAME, "ChaincodeId");
		environment.put(LauncherConfiguration.CORE_PEER_ADDRESS, "1.1.1.1:1111");
		environment.put(LauncherConfiguration.CORE_PEER_TLS_ENABLED, "true");
		environment.put(LauncherConfiguration.CORE_PEER_TLS_SERVERHOSTOVERRIDE, "hostOverride");
		environment.put(LauncherConfiguration.CORE_PEER_TLS_ROOTCERT_FILE, "certPath");
		configuration.load(new String[0]);
		assertEquals("1.1.1.1", configuration.getHost());
		assertEquals(1111, configuration.getPort());
		assertTrue(configuration.isTlsEnabled());
		assertEquals("hostOverride", configuration.getHostOverrideAuthority());
		assertEquals("ChaincodeId", configuration.getId());
		assertEquals("certPath", configuration.getRootCertFile());
	}

	@Test
	public void loadCommandLineOverSystemEnv() throws Exception {
		when(environment.containsKey(anyString())).thenReturn(true);
		environment.put(LauncherConfiguration.CORE_CHAINCODE_ID_NAME, "ChaincodeId");
		environment.put(LauncherConfiguration.CORE_PEER_ADDRESS, "1.1.1.1:1111");
		environment.put(LauncherConfiguration.CORE_PEER_TLS_ENABLED, "false");
		environment.put(LauncherConfiguration.CORE_PEER_TLS_SERVERHOSTOVERRIDE, "hostOverride");
		environment.put(LauncherConfiguration.CORE_PEER_TLS_ROOTCERT_FILE, "certPath");
		configuration.load(new String[]{"-a", "2.2.2.2:2222", "-i", "newChaincodeId", "-s", "-o", "newHostOverride"});
		assertEquals(configuration.getHost(), "2.2.2.2");
		assertEquals(configuration.getPort(), 2222);
		assertTrue(configuration.isTlsEnabled());
		assertEquals(configuration.getHostOverrideAuthority(), "newHostOverride");
		assertEquals(configuration.getId(), "newChaincodeId");
		assertEquals(configuration.getRootCertFile(), "certPath");
	}
}