package org.hyperledger.fabric.shim.mock.peer;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

import java.util.ArrayList;
import java.util.List;

public class PutValueState implements ScenarioStep {
    ChaincodeShim.ChaincodeMessage orgMsg;
    @Override
    public boolean expected(ChaincodeShim.ChaincodeMessage msg) {
        orgMsg = msg;
        return msg.getType() == ChaincodeShim.ChaincodeMessage.Type.PUT_STATE;
    }

    @Override
    public List<ChaincodeShim.ChaincodeMessage> next() {
        List<ChaincodeShim.ChaincodeMessage> list = new ArrayList<>();
        list.add(ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.RESPONSE)
                .setChannelId(orgMsg.getChannelId())
                .setTxid(orgMsg.getTxid())
                .build());
        return list;
    }

    @Override
    public long waitPeriod() {
        return 0;
    }
}