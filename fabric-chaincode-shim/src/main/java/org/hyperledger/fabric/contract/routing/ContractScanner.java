/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.routing;

import org.hyperledger.fabric.contract.execution.InvocationRequest;

public interface ContractScanner {

    void findAndSetContracts() throws IllegalAccessException, InstantiationException;

    Routing getRouting(InvocationRequest req);

    Routing getDefaultRouting(InvocationRequest req);
}
