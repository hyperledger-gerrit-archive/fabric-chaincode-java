package org.hyperledger.fabric.shim.mock.peer;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

import java.util.ArrayList;
import java.util.List;

public class CompleteStep implements ScenarioStep {
    @Override
    public boolean expected(ChaincodeShim.ChaincodeMessage msg) {
        return msg.getType() == ChaincodeShim.ChaincodeMessage.Type.COMPLETED;
    }

    @Override
    public List<ChaincodeShim.ChaincodeMessage> next() {
        List<ChaincodeShim.ChaincodeMessage> list = new ArrayList<>();
        return list;
    }

    @Override
    public long waitPeriod() {
        return 0;
    }
}