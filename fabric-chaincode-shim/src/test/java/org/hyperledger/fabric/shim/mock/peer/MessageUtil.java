package org.hyperledger.fabric.shim.mock.peer;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.shim.Chaincode;

public class MessageUtil {

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

    public static ProposalResponsePackage.Response toProtoResponse(Chaincode.Response response) {
        final ProposalResponsePackage.Response.Builder builder = ProposalResponsePackage.Response.newBuilder();
        builder.setStatus(response.getStatus().getCode());
        if (response.getMessage() != null) {
            builder.setMessage(response.getMessage());
        }
        if (response.getPayload() != null) {
            builder.setPayload(ByteString.copyFrom(response.getPayload()));
        }
        return builder.build();
    }


}
