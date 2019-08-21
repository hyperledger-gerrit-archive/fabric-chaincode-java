/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.metrics.impl;

import org.hyperledger.fabric.metrics.MetricsProvider;

/**
 * Simple default provider that logs to the org.hyperledger.Performance logger the basic metrics
 *
 */
public class NullProvider implements MetricsProvider {
    
	public NullProvider() {	}
	
}