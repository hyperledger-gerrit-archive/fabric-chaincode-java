/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import org.hyperledger.fabric.contract.execution.ExecutionFactory;
import org.hyperledger.fabric.contract.execution.ExecutionService;
import org.hyperledger.fabric.contract.execution.InvocationRequest;
import org.hyperledger.fabric.contract.routing.ContractScanner;
import org.hyperledger.fabric.contract.routing.Routing;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.hyperledger.fabric.contract.routing.impl.ContractScannerImpl;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Router class routes Init/Invoke requests to contracts.
 * Implements {@link org.hyperledger.fabric.shim.Chaincode} interface.
 */
public class ContractRouter extends ChaincodeBase {

    private ContractScanner scanner;
    private ExecutionService executor;

    public ContractRouter() {
        scanner = new ContractScannerImpl();
        executor = ExecutionFactory.getInstance().createExecutionService();
    }

    public void findAllContracts() {
        try {
            scanner.findAndSetContracts();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void startRouting(String[] args) {
        start(args);
    }

    @Override
    public Response init(ChaincodeStub stub) {
        InvocationRequest request = ExecutionFactory.getInstance().createRequest(stub);
        Routing routing = scanner.getRouting(request);
        if (routing == null) {
            routing = scanner.getDefaultRouting(request);
        }
        if (routing != null) {
            if (routing.getType() == TransactionType.INIT || routing.getType() == TransactionType.DEFAULT) {
                return executor.executeRequest(routing, request);
            } else {
                return newErrorResponse("Can't find @Init method " + request.getMethod() + " in namespace " + request.getNamespace() + " and no default method as well");
            }
        }
        return newErrorResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        InvocationRequest request = ExecutionFactory.getInstance().createRequest(stub);
        Routing routing = scanner.getRouting(request);
        if (routing == null) {
            routing = scanner.getDefaultRouting(request);
        }
        if (routing != null) {
            if (routing.getType() == TransactionType.INVOKE || routing.getType() == TransactionType.QUERY || routing.getType() == TransactionType.DEFAULT) {
                return executor.executeRequest(routing, request);
            } else {
                return newErrorResponse("Can't find @Transaction method " + request.getMethod() + " in namespace " + request.getNamespace() + " and no default method as well");
            }
        }
        return newErrorResponse();
    }

    public static void main(String[] args) {
        ContractRouter cfc = new ContractRouter();
        cfc.findAllContracts();
        cfc.startRouting(args);
    }
}
