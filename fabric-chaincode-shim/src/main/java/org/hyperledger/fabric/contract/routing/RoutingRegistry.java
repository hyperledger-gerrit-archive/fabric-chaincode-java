/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.ContractRuntimeException;
import org.hyperledger.fabric.contract.execution.InvocationRequest;
import org.hyperledger.fabric.contract.routing.impl.ContractDefinitionImpl;
import org.hyperledger.fabric.contract.routing.impl.ContractScannerImpl;

/**
 * Registry to hold permit access to the routing definitions.
 * This is the primary internal data structure to permit access to information
 * about the contracts, and their transaction functions.
 *
 * Contracts are added, and processed. At runtime, this can then be accessed to locate
 * a specific 'Route' that can be handed off to the ExecutionService
 *
 */
public class RoutingRegistry {
	private static Logger logger = Logger.getLogger(RoutingRegistry.class);

	private static Map<String, ContractDefinition> contracts = new HashMap<>();

	/**
	 * Add a new contract definition based on the class object located
	 *
	 * @param clz Class Object to process into a ContractDefinition
	 * @return ContractDefinition Instance
	 */
	public static ContractDefinition addNewContract(Class<?> clz) {
		logger.debug(()->"Adding new Contract Class "+clz.getCanonicalName());
		ContractDefinition contract;
		contract = new ContractDefinitionImpl(clz);

		// index this by the full qualified name
		contracts.put(contract.getName(), contract);
		if (contract.isDefault()) {
			contracts.put(InvocationRequest.DEFAULT_NAMESPACE, contract);
		}

		return contract;
	}

	/**
	 * Based on the Invocation Request, can we create a route for this?
	 *
	 * @param request
	 * @return
	 */
	public static boolean containsRoute(InvocationRequest request) {
		if (contracts.containsKey(request.getNamespace())) {
			ContractDefinition cd = contracts.get(request.getNamespace());

			if (cd.hasTxFunction(request.getMethod())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the route for invocation request
	 * @param request
	 * @return
	 */
	public static Routing getRoute(InvocationRequest request) {
		TxFunction txFunction = contracts.get(request.getNamespace()).getTxFunction(request.getMethod());
		return txFunction.getRouting();
	}


	public static ContractDefinition getContract(String namespace) {
		return contracts.get(namespace);
	}

	/**
	 * Returns all the ContractDefinitions for this registry
	 * @return
	 */
	public static Collection<ContractDefinition> getAllDefinitions(){
		return contracts.values();

	}

}
