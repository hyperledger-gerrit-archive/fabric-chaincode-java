/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.execution;

import org.hyperledger.fabric.contact.routing.Routing;
import org.hyperledger.fabric.shim.Chaincode;

public interface ExecutionService {

    Chaincode.Response executeRequest(Routing rd, InvocationRequest req);
}
