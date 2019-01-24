/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.routing;

import org.hyperledger.fabric.contact.execution.ExecutionService;
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
        RoutingData routing = scanner.getRouting(stub);
        if (routing == null) {
            //TODO: take care of default method
        } else {
            if (routing.type == RoutingData.TransactionType.INIT || routing.type == RoutingData.TransactionType.UPGRADE) {
                return executor.executeRequest(scanner.getRouting(stub), stub);
            } else {
                //TODO: Take care error message - method annotation, expected Init/Upgrade
            }
        }
        //TODO: Take care error message - no method found
        return newErrorResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        RoutingData routing = scanner.getRouting(stub);
        if (routing == null) {
            //TODO: take care of default method
        } else {
            if (routing.type == RoutingData.TransactionType.INVOKE || routing.type == RoutingData.TransactionType.QUERY) {
                return executor.executeRequest(scanner.getRouting(stub), stub);
            } else {
                //TODO: Take care error message - method annotation, expected Init/Upgrade
            }
        }
        //TODO: Take care error message - no method found
        return newErrorResponse();
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        ContractScanner scanner = new ContractScanner();
        scanner.findAndSetChaincode();
        ExecutionService executionService = null;
        new ContractFromChaincode(scanner, executionService).start(args);
    }
}
