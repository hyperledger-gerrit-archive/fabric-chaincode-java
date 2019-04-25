/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.execution.ExecutionFactory;
import org.hyperledger.fabric.contract.execution.ExecutionService;
import org.hyperledger.fabric.contract.execution.InvocationRequest;
import org.hyperledger.fabric.contract.metadata.MetadataBuilder;
import org.hyperledger.fabric.contract.routing.ContractDefinition;
import org.hyperledger.fabric.contract.routing.ContractScanner;
import org.hyperledger.fabric.contract.routing.Routing;
import org.hyperledger.fabric.contract.routing.RoutingRegistry;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.hyperledger.fabric.contract.routing.impl.ContractScannerImpl;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ResponseUtils;

/**
 * Router class routes Init/Invoke requests to contracts. Implements
 * {@link org.hyperledger.fabric.shim.Chaincode} interface.
 */
public class ContractRouter extends ChaincodeBase {
	private static Logger logger = Logger.getLogger(ContractRouter.class.getName());

	private ContractScanner scanner;
	private ExecutionService executor;

	/**
	 * Take the arguments from the cli, and initiate processing of cli options and
	 * environment variables.
	 * 
	 * Create the Contract scanner, and the Execution service
	 * 
	 * @param args
	 */
	public ContractRouter(String[] args) {
		super.initializeLogging();
		super.processEnvironmentOptions();
		super.processCommandLineOptions(args);

		super.validateOptions();
		scanner = new ContractScannerImpl();
		executor = ExecutionFactory.getInstance().createExecutionService();
	}

	/**
	 * Locate all the contracts that are available on the classpath
	 */
	void findAllContracts() {
		try {
			scanner.findAndSetContracts();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Start the chaincode container off and running, this will send the initial
	 * flow back to the peer
	 * 
	 * @throws Exception
	 */
	void startRouting() throws Exception {
		super._start();
	}

	@Override
	public Response init(ChaincodeStub stub) {
		InvocationRequest request = ExecutionFactory.getInstance().createRequest(stub);
		Routing routing = getRouting(request);
		if (routing != null) {
			if (routing.getType() == TransactionType.INIT || routing.getType() == TransactionType.DEFAULT) {
				return executor.executeRequest(routing, request, stub);
			}
		}
		return ResponseUtils.newErrorResponse("Can't find @Init method " + request.getMethod() + " in namespace "
				+ request.getNamespace() + " and no default method as well");
	}

	@Override
	public Response invoke(ChaincodeStub stub) {
		logger.debug(() -> "Got the invocations:" + stub.getFunction() + " " + stub.getParameters());
		InvocationRequest request = ExecutionFactory.getInstance().createRequest(stub);
		Routing routing = getRouting(request);

		logger.debug(() -> "Got routing:" + routing);

		if (routing != null) {
			if (routing.getType() == TransactionType.INVOKE || routing.getType() == TransactionType.QUERY
					|| routing.getType() == TransactionType.DEFAULT) {
				return executor.executeRequest(routing, request, stub);
			}
		}
		return ResponseUtils.newErrorResponse("Can't find @Transaction method " + request.getMethod() + " in namespace "
				+ request.getNamespace() + " and no default method as well");
	}

	/**
	 * Given the Invocation Request, return the routing object for this call
	 * 
	 * @param request
	 * @return
	 */
	Routing getRouting(InvocationRequest request) {
    	//request name is the fully qualified 'name:txname'
        if (RoutingRegistry.containsRoute(request)) {
            return RoutingRegistry.getRoute(request);
        } else {
        	ContractDefinition contract = RoutingRegistry.getContract(request.getNamespace());
        	return contract.getUnkownRoute();
        }
	}

	/**
	 * Main method to start the contract based chaincode
	 *
	 */
	public static void main(String[] args) {

		ContractRouter cfc = new ContractRouter(args);
		cfc.findAllContracts();

		// Create the static metadata ahead of time rather than have to produce every
		// time
		MetadataBuilder.initialize();
		logger.info(() -> "Metadata follows:" + MetadataBuilder.debugString());

		try {
			// commence routing, once this has returned the chaincode and contract api is
			// 'open for business'
			cfc.startRouting();
		} catch (Exception e) {
			logger.severe("Chaincode could not start" + e);
		}
	}
}
