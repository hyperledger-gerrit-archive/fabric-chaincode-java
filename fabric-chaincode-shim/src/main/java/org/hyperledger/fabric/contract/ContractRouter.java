/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import org.hyperledger.fabric.contract.execution.ExecutionService;
import org.hyperledger.fabric.contract.routing.ContractScanner;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

public class ContractRouter extends ChaincodeBase {

    ContractScanner scanner;
    ExecutionService executor;

    public ContractRouter() {
    }

    @Override
    public Response init(ChaincodeStub stub) {
        return newErrorResponse();
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        return newErrorResponse();
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        ContractRouter cfc = new ContractRouter();
        cfc.scanner.findAndSetContracts();
        cfc.start(args);
    }
}
