/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.routing;

import org.hyperledger.fabric.contact.execution.InvocationRequest;

public interface ContractScanner {

    void findAndSetContracts() throws IllegalAccessException, InstantiationException;

    Routing getRouting(InvocationRequest req);

    Routing getDefaultRouting(InvocationRequest req);
}
