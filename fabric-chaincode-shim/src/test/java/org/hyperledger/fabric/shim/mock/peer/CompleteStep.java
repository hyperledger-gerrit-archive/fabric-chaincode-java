/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim.mock.peer;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

import java.util.Collections;
import java.util.List;

public class CompleteStep implements ScenarioStep {
    @Override
    public boolean expected(ChaincodeShim.ChaincodeMessage msg) {
        return msg.getType() == ChaincodeShim.ChaincodeMessage.Type.COMPLETED;
    }

    @Override
    public List<ChaincodeShim.ChaincodeMessage> next() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public long waitPeriod() {
        return 0;
    }
}
