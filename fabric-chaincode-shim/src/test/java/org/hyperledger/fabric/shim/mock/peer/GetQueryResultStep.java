/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.mock.peer;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

public class GetQueryResultStep extends QueryResultStep {

    public GetQueryResultStep(boolean hasNext, String... vals) {
        super(hasNext, vals);
    }

    @Override
    public boolean expected(ChaincodeShim.ChaincodeMessage msg) {
        super.orgMsg = msg;
        return msg.getType() == ChaincodeShim.ChaincodeMessage.Type.GET_QUERY_RESULT;
    }

    @Override
    public long waitPeriod() {
        return 0;
    }
}
