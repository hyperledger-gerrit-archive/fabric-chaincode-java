/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.mock.peer;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.shim.Chaincode;

import java.util.ArrayList;
import java.util.List;

public class InvokeChaincodeStep implements ScenarioStep {
    ChaincodeShim.ChaincodeMessage orgMsg;

    @Override
    public boolean expected(ChaincodeShim.ChaincodeMessage msg) {
        orgMsg = msg;
        return msg.getType() == ChaincodeShim.ChaincodeMessage.Type.INVOKE_CHAINCODE;
    }

    @Override
    public List<ChaincodeShim.ChaincodeMessage> next() {
        ByteString chaincodeResponse = ProposalResponsePackage.Response.newBuilder()
                .setStatus(Chaincode.Response.Status.SUCCESS.getCode())
                .setMessage("OK")
                .build().toByteString();
        ByteString getPayload = ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.COMPLETED)
                .setChannelId(orgMsg.getChannelId())
                .setTxid(orgMsg.getTxid())
                .setPayload(chaincodeResponse)
                .build().toByteString();
        List<ChaincodeShim.ChaincodeMessage> list = new ArrayList<>();
        list.add(ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.RESPONSE)
                .setChannelId(orgMsg.getChannelId())
                .setTxid(orgMsg.getTxid())
                .setPayload(getPayload)
                .build());
        return list;
    }

    @Override
    public long waitPeriod() {
        return 0;
    }
}
