/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.mock.peer;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

public class QueryNextStep extends QueryResultStep {

    public QueryNextStep(boolean hasNext, String... vals) {
        super(hasNext, vals);
    }

    @Override
    public boolean expected(ChaincodeShim.ChaincodeMessage msg) {
        super.orgMsg = msg;
        return msg.getType() == ChaincodeShim.ChaincodeMessage.Type.QUERY_STATE_NEXT;
    }

    @Override
    public long waitPeriod() {
        return 0;
    }
}
