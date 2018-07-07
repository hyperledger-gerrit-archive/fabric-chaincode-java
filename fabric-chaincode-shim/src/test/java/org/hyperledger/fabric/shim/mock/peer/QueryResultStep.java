/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.mock.peer;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public abstract class QueryResultStep implements ScenarioStep {
    ChaincodeShim.ChaincodeMessage orgMsg;
    String[] values;
    boolean hasNext;

    QueryResultStep(boolean hasNext, String... vals) {
        this.values = vals;
        this.hasNext = hasNext;
    }

    @Override
    public List<ChaincodeShim.ChaincodeMessage> next() {
        List<KvQueryResult.KV> keyValues = Arrays.asList(values).stream().
                map(x -> KvQueryResult.KV.newBuilder()
                        .setKey(x)
                        .setValue(ByteString.copyFromUtf8(x + " Value"))
                        .build()
                ).collect(toList());

        ChaincodeShim.QueryResponse.Builder builder = ChaincodeShim.QueryResponse.newBuilder();
        builder.setHasMore(hasNext);
        keyValues.stream().forEach(kv -> builder.addResults(ChaincodeShim.QueryResultBytes.newBuilder().setResultBytes(kv.toByteString())));
        ByteString rangePayload = builder.build().toByteString();

        List<ChaincodeShim.ChaincodeMessage> list = new ArrayList<>();
        list.add(ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.RESPONSE)
                .setChannelId(orgMsg.getChannelId())
                .setTxid(orgMsg.getTxid())
                .setPayload(rangePayload)
                .build());
        return list;
    }
}
