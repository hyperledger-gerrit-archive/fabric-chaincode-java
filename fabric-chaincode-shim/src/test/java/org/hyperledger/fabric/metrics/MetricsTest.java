
/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.hyperledger.fabric.metrics.Metrics.DefaultProvider;
import org.hyperledger.fabric.metrics.Metrics.TaskMetricsCollector;
import org.hyperledger.fabric.shim.impl.InnvocationTaskExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MetricsTest {

    static class TestProvider implements MetricsProvider {

        public TestProvider() {

        }

        @Override
        public void setTaskMetricsCollector(TaskMetricsCollector taskService) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void initialize(Map<String, String> props) {
            // TODO Auto-generated method stub
            
        }

    }

    @Nested
    @DisplayName("Metrics initialize")
    class Initalize {

        @Test
        public void metricsDisabled() {
            HashMap<String, String> props = new HashMap<String, String>();
            MetricsProvider provider = Metrics.initialize(props);
            assertTrue(provider instanceof DefaultProvider);
        }

        @Test
        public void metricsEnabledUnkownProvider() {
            HashMap<String, String> props = new HashMap<String, String>();
            props.put("CHAINCODE_METRICS_PROVIDER", "org.acme.metrics.provider");
            props.put("CHAINCODE_METRICS_ENABLED", "true");

            assertThrows(RuntimeException.class, () -> {
                MetricsProvider provider = Metrics.initialize(props);
            }, "Unable to start metrics");
        }

        @Test
        public void metricsNoProvider() {
            HashMap<String, String> props = new HashMap<String, String>();
            props.put("CHAINCODE_METRICS_ENABLED", "true");

            MetricsProvider provider = Metrics.initialize(props);
            assertTrue(provider instanceof DefaultProvider);
        }

        @Test
        public void metricsValid() {
            HashMap<String, String> props = new HashMap<String, String>();
            props.put("CHAINCODE_METRICS_PROVIDER", MetricsTest.TestProvider.class.getName());
            props.put("CHAINCODE_METRICS_ENABLED", "true");
            MetricsProvider provider = Metrics.initialize(props);

            assertThat(provider).isExactlyInstanceOf(MetricsTest.TestProvider.class);
        }

    }

    @Nested
    @DisplayName("Default Metrics Provider")
    class DefaultProviderTest {

        @Test
        public void allMethods() {
            MetricsProvider provider = new DefaultProvider();
            provider.setTaskMetricsCollector(new TaskMetricsCollector() {

                @Override
                public int getPoolSize() {
                    return 0;
                }

                @Override
                public int getMaximumPoolSize() {
                    return 0;
                }

                @Override
                public int getLargestPoolSize() {
                    return 0;
                }

                @Override
                public int getCurrentTaskCount() {
                    return 0;
                }

                @Override
                public int getCurrentQueueCount() {
                    return 0;
                }

                @Override
                public int getCorePoolSize() {
                    return 0;
                }

                @Override
                public int getActiveCount() {
                    // TODO Auto-generated method stub
                    return 0;
                }
            });

        }
    }

}
