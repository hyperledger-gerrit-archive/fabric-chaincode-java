/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.execution;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.execution.impl.ContractExecutionService;
import org.hyperledger.fabric.contract.execution.impl.ContractInvocationRequest;

public class ExecutionFactory {
    static ExecutionFactory rf;
    static ExecutionService es;

    public static ExecutionFactory getInstance() {
        if (rf == null) {
            rf = new ExecutionFactory();
        }
        return rf;
    }

    public InvocationRequest createRequest(Context context) {
        return new ContractInvocationRequest(context);
    }

    public ExecutionService createExecutionService() {
        if (es == null) {
            es = new ContractExecutionService();
        }
        return es;
    }
}
