/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact;

import org.hyperledger.fabric.contact.execution.ExecutionFactory;
import org.hyperledger.fabric.contact.execution.ExecutionService;
import org.hyperledger.fabric.contact.execution.InvocationRequest;
import org.hyperledger.fabric.contact.routing.ContractScanner;
import org.hyperledger.fabric.contact.routing.Routing;
import org.hyperledger.fabric.contact.routing.impl.ContractScannerImpl;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

public class ContractFromChaincode extends ChaincodeBase {

    ContractScanner scanner;
    ExecutionService executor;

    public ContractFromChaincode(ContractScanner scanner, ExecutionService executor) {
        this.scanner = scanner;
        this.executor = executor;

    }

    @Override
    public Response init(ChaincodeStub stub) {
        Context context = ContextFactory.getInstance().createContext(stub);
        InvocationRequest request = ExecutionFactory.getInstance().createRequest(context);
        Routing routing = scanner.getRouting(request);
        if (routing == null) {
            routing = scanner.getDefaultRouting(request);
        }
        if (routing != null) {
            if (routing.getType() == Routing.TransactionType.INIT || routing.getType() == Routing.TransactionType.UPGRADE || routing.getType() == Routing.TransactionType.DEFAULT) {
                return executor.executeRequest(routing, request);
            } else {
                return newErrorResponse("Can't find @Init or @Update method " + request.getMethod() + " in namespace " + request.getNamespace() + " and no default method as well");
            }
        }
        return newErrorResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        Context context = ContextFactory.getInstance().createContext(stub);
        InvocationRequest request = ExecutionFactory.getInstance().createRequest(context);
        Routing routing = scanner.getRouting(request);
        if (routing == null) {
            routing = scanner.getDefaultRouting(request);
        }
        if (routing != null) {
            if (routing.getType() == Routing.TransactionType.INVOKE || routing.getType() == Routing.TransactionType.QUERY || routing.getType() == Routing.TransactionType.DEFAULT) {
                return executor.executeRequest(routing, request);
            } else {
                return newErrorResponse("Can't find @Transaction method " + request.getMethod() + " in namespace " + request.getNamespace() + " and no default method as well");
            }
        }
        return newErrorResponse();
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        ContractScanner scanner = new ContractScannerImpl();
        scanner.findAndSetContracts();
        ExecutionService executionService = ExecutionFactory.getInstance().createExecutionService();
        new ContractFromChaincode(scanner, executionService).start(args);
    }
}
