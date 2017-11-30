/*
 * Copyright IBM Corp., DTCC All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.shim.impl;

import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LauncherConfiguration {
	private Map<String,String> environment = System.getenv();

	private static Log logger = LogFactory.getLog(LauncherConfiguration.class);

	static final String DEFAULT_HOST = "127.0.0.1";
	static final int DEFAULT_PORT = 7051;

	private String host = DEFAULT_HOST;
	private int port = DEFAULT_PORT;
	private String hostOverrideAuthority = "";
	private boolean tlsEnabled = false;
	private String rootCertFile = "/etc/hyperledger/fabric/peer.crt";

	private String id;

	final static String CORE_CHAINCODE_ID_NAME = "CORE_CHAINCODE_ID_NAME";
	final static String CORE_PEER_ADDRESS = "CORE_PEER_ADDRESS";
	final static String CORE_PEER_TLS_ENABLED = "CORE_PEER_TLS_ENABLED";
	final static String CORE_PEER_TLS_SERVERHOSTOVERRIDE = "CORE_PEER_TLS_SERVERHOSTOVERRIDE";
	final static String CORE_PEER_TLS_ROOTCERT_FILE = "CORE_PEER_TLS_ROOTCERT_FILE";

	void load(String[] args) {
		processEnvironmentOptions();
		processCommandLineOptions(args);
	}

	private void processCommandLineOptions(String[] args) {
		Options options = new Options();
		options.addOption("a", "peer.address", true, "Address of peer to connect to");
		options.addOption(null, "peerAddress", true, "Address of peer to connect to");
		options.addOption("s", "securityEnabled", false, "Present if security is enabled");
		options.addOption("i", "id", true, "Identity of chaincode");
		options.addOption("o", "hostNameOverride", true, "Hostname override for server certificate");
		try {
			CommandLine cl = new DefaultParser().parse(options, args);
			if (cl.hasOption('a')) {
				setPeerAddress(cl.getOptionValue('a', DEFAULT_HOST));
			} else if (cl.hasOption("peerAddress")) {
        setPeerAddress(cl.getOptionValue("peerAddress", DEFAULT_HOST));
      }
			if (cl.hasOption('s')) {
				tlsEnabled = true;
				logger.info("TLS enabled");
				if (cl.hasOption('o')) {
					hostOverrideAuthority = cl.getOptionValue('o');
					logger.info("server host override given " + hostOverrideAuthority);
				}
			}
			if (cl.hasOption('i')) {
				id = cl.getOptionValue('i');
			}
		} catch (Exception e) {
			logger.warn("cli parsing failed with exception", e);

		}
	}

	private void processEnvironmentOptions() {
		if (environment.containsKey(CORE_CHAINCODE_ID_NAME)) {
			this.id = environment.get(CORE_CHAINCODE_ID_NAME);
		}
		if (environment.containsKey(CORE_PEER_ADDRESS)) {
			setPeerAddress(environment.get(CORE_PEER_ADDRESS));
		}
		if (environment.containsKey(CORE_PEER_TLS_ENABLED)) {
			this.tlsEnabled = Boolean.parseBoolean(environment.get(CORE_PEER_TLS_ENABLED));
			if (environment.containsKey(CORE_PEER_TLS_SERVERHOSTOVERRIDE)) {
				this.hostOverrideAuthority = environment.get(CORE_PEER_TLS_SERVERHOSTOVERRIDE);
			}
			if (environment.containsKey(CORE_PEER_TLS_ROOTCERT_FILE)) {
				this.rootCertFile = environment.get(CORE_PEER_TLS_ROOTCERT_FILE);
			}
		}
	}

	private void setPeerAddress(String peerAddress) {
		String[] tokens = peerAddress.split(":");
		port = new Integer(tokens[1]);
		host = tokens[0];
	}

	String getId() {
		return id;
	}

	String getHost() {
		return host;
	}

	int getPort() {
		return port;
	}

	String getHostOverrideAuthority() {
		return hostOverrideAuthority;
	}

	boolean isTlsEnabled() {
		return tlsEnabled;
	}

	String getRootCertFile() {
		return rootCertFile;
	}
}
