/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact;

import org.hyperledger.fabric.contact.execution.ExecutionService;
import org.hyperledger.fabric.contact.routing.ContractScanner;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

public class ContractFromChaincode extends ChaincodeBase {

    ContractScanner scanner;
    ExecutionService executor;

    public ContractFromChaincode() {
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
        ContractFromChaincode cfc = new ContractFromChaincode();
        cfc.scanner.findAndSetContracts();
        cfc.start(args);
    }
}
