/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.metrics;
import org.hyperledger.fabric.metrics.Metrics.TaskMetricsCollector;

public interface MetricsProvider {

	void setIdentifier(String id);

	/**
	 * Pass a reference to this service for information gathering
	 *
	 * @param taskService
	 */
	void  setTaskMetricsCollector(TaskMetricsCollector taskService);

}
