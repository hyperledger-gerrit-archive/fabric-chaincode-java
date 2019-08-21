/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.metrics.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.hyperledger.fabric.metrics.Metrics;
import org.hyperledger.fabric.metrics.Metrics.DefaultProvider;
import org.hyperledger.fabric.metrics.MetricsProvider;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.client.hotspot.BufferPoolsExports;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryAllocationExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;
import io.prometheus.client.hotspot.VersionInfoExports;

public class PrometheusProvider extends DefaultProvider implements MetricsProvider {

	private CollectorRegistry registry;
	private PushGateway pg;
	private String id;
	private Metrics.TaskMetricsCollector taskService;
	private String peerName;

	public void setTaskMetricsCollector(Metrics.TaskMetricsCollector taskService) {
	    this.taskService = taskService;
	}

	private final static String PROMETHEUS_PORT = "PROMETHEUS_PORT";
	private final static String PROMETHEUS_HOST = "PROMETHEUS_HOST";

	class ThreadPoolExports extends Collector {
		@Override
		public List<MetricFamilySamples> collect() {
			List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
			if (PrometheusProvider.this.taskService != null) { 			     
	            GaugeMetricFamily gmf = new GaugeMetricFamily("cc_tx_pool","Chaincode Transaction Pool",Collections.singletonList("metric"));
			    gmf.addMetric(Collections.singletonList("active_count"), PrometheusProvider.this.taskService.getActiveCount());
			    gmf.addMetric(Collections.singletonList("pool_size"), PrometheusProvider.this.taskService.getPoolSize()); 
			    gmf.addMetric(Collections.singletonList("core_pool_size"), PrometheusProvider.this.taskService.getCorePoolSize()); 
			    gmf.addMetric(Collections.singletonList("current_task_count"), PrometheusProvider.this.taskService.getCurrentTaskCount());
			    gmf.addMetric(Collections.singletonList("current_queue_depth"), PrometheusProvider.this.taskService.getCurrentQueueCount());
			    mfs.add(gmf);
			}
			return mfs;
		}
	}

	public PrometheusProvider() {
	   
	}
	
	@Override
	public void initialize(Map<String, String> props) {
		this.registry = new CollectorRegistry();
		
		int port = 9091;
		String host = "pushGateway";
		if (props.containsKey(PROMETHEUS_PORT)) {
			port = Integer.parseInt((props.get(PROMETHEUS_PORT)));
		}

		if (props.containsKey(PROMETHEUS_HOST)) {
			host = (props.get(PROMETHEUS_HOST));
		}
		this.pg = new PushGateway(host + ":" + port);
		this.id = props.get("CORE_CHAINCODE_ID_NAME").replace(':','_');
		this.peerName = props.get("CORE_PEER_ADDRESS");
		
		new ThreadPoolExports().register(registry);

		// setup the standard JVM ports etc.
		new StandardExports().register(registry);
		new MemoryPoolsExports().register(registry);
		new MemoryAllocationExports().register(registry);
		new BufferPoolsExports().register(registry);
		new GarbageCollectorExports().register(registry);
		new ThreadExports().register(registry);
		new ClassLoadingExports().register(registry);
		new VersionInfoExports().register(registry);

		Timer metricTimer = new Timer(true);
		metricTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				PrometheusProvider.this.pushMetrics();
			}
		}, 0, 5000);

	}

	protected final void pushMetrics() {

		try {
		    Map<String,String> map = new HashMap<String,String>();
		    map.put("CORE_PEER_ADDRESS", this.peerName);
			pg.pushAdd(registry, id,map);
		} catch (IOException e) {
			// succesful response do sometimes throw exceptions
			System.out.println(e.getMessage());
		}
	}
}
