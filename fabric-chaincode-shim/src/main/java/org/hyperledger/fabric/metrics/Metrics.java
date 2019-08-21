/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.metrics;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.logging.Logger;

import org.hyperledger.fabric.Logging;
import org.hyperledger.fabric.metrics.impl.DefaultProvider;

/**
 * Metrics setups up the provider in use from the configuration supplied
 * If not enabled, nothing happens, but if enabled but no specific logger default is used
 * that uses the org.hyperledger.Performance logger
 */
public class Metrics {
    
    private static final String CHAINCODE_METRICS_ENABLED = "CHAINCODE_METRICS_ENABLED";
    private static final String CHAINCODE_METRICS_PROVIDER = "CHAINCODE_METRICS_PROVIDER";

    private static Logger logger = Logger.getLogger(Metrics.class.getName());

    private static MetricsProvider provider;

    public static MetricsProvider initialize(Map<String,String> props) {
        if ( Boolean.parseBoolean(props.get(CHAINCODE_METRICS_ENABLED))) {
            try {
                logger.info("Metrics enabled");
                if (props.containsKey(CHAINCODE_METRICS_PROVIDER)){
                    String providerClass = props.get(CHAINCODE_METRICS_PROVIDER);

                    @SuppressWarnings("unchecked")
					Class<MetricsProvider> clazz = (Class<MetricsProvider>) Class.forName(providerClass);
                    provider = (MetricsProvider) clazz.getConstructor().newInstance();
                    provider.initialize(props);
                } else {
                    logger.info("Using default metrics provider (logs to org.hyperledger.Performance)");
                    provider = new DefaultProvider();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException("Unable to start metrics",e);
            }
        } else {
            // return a 'null' provider
        	logger.info("Metrics disabled");
            provider = new MetricsProvider() {};
        }

        return provider;
    }

    public static MetricsProvider getProvider() {
        return provider;
    }
    
}
