/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim.utils;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.shim.Chaincode;

public class MessageUtil {
    /**
     * Generate chaincode messages
     * @param type
     * @param channelId
     * @param txId
     * @param payload
     * @param event
     * @return
     */
    public static ChaincodeShim.ChaincodeMessage newEventMessage(final ChaincodeShim.ChaincodeMessage.Type type, final String channelId, final String txId, final ByteString payload, final ChaincodeEventPackage.ChaincodeEvent event) {
        if (event == null) {
            ChaincodeShim.ChaincodeMessage chaincodeMessage = ChaincodeShim.ChaincodeMessage.newBuilder()
                    .setType(type)
                    .setChannelId(channelId)
                    .setTxid(txId)
                    .setPayload(payload)
                    .build();
            return chaincodeMessage;
        } else {
            return ChaincodeShim.ChaincodeMessage.newBuilder()
                    .setType(type)
                    .setChannelId(channelId)
                    .setTxid(txId)
                    .setPayload(payload)
                    .setChaincodeEvent(event)
                    .build();
        }
    }
}
