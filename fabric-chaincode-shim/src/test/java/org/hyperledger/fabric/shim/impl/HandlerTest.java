/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.impl;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class HandlerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testHandlerStates() {
        ChaincodeBase cb = new ChaincodeBase() {
            @Override
            public Response init(ChaincodeStub stub) {
                return newSuccessResponse();
            }

            @Override
            public Response invoke(ChaincodeStub stub) {
                return newSuccessResponse();
            }
        };
        Chaincode.ChaincodeID chaincodeId = Chaincode.ChaincodeID.newBuilder().setName("mycc").build();
        Handler handler = new Handler(chaincodeId, cb);

        ChaincodeShim.ChaincodeMessage msgReg = ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.REGISTERED)
                .build();
        // Correct message
        handler.onChaincodeMessage(msgReg);
        Assert.assertEquals("Not correct handler state", Handler.CCState.ESTABLISHED, handler.getState());

        ChaincodeShim.ChaincodeMessage msgReady = ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.READY)
                .build();
        // Correct message
        handler.onChaincodeMessage(msgReady);
        Assert.assertEquals("Not correct handler state", Handler.CCState.READY, handler.getState());

        handler = new Handler(chaincodeId, cb);
        // Incorrect message
        handler.onChaincodeMessage(msgReady);
        Assert.assertEquals("Not correct handler state", Handler.CCState.CREATED, handler.getState());
        // Correct message
        handler.onChaincodeMessage(msgReg);
        Assert.assertEquals("Not correct handler state", Handler.CCState.ESTABLISHED, handler.getState());
        // Incorrect message
        handler.onChaincodeMessage(msgReg);
        Assert.assertEquals("Not correct handler state", Handler.CCState.ESTABLISHED, handler.getState());
        handler.onChaincodeMessage(msgReady);
        Assert.assertEquals("Not correct handler state", Handler.CCState.READY, handler.getState());

        ChaincodeShim.ChaincodeMessage errorMsg = ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.ERROR)
                .setChannelId("mychannel")
                .setTxid("q")
                .setPayload(ByteString.copyFromUtf8(""))
                .build();
        // Error message, except exception, no open communication
        thrown.expect(IllegalStateException.class);
        handler.onChaincodeMessage(errorMsg);
        Assert.assertEquals("Not correct handler state", Handler.CCState.READY, handler.getState());

        ChaincodeShim.ChaincodeMessage unkonwnMessage = ChaincodeShim.ChaincodeMessage.newBuilder()
                .setType(ChaincodeShim.ChaincodeMessage.Type.PUT_STATE)
                .setChannelId("mychannel")
                .setTxid("q")
                .setPayload(ByteString.copyFromUtf8(""))
                .build();

        // Unrelated message, do nothing
        handler.onChaincodeMessage(unkonwnMessage);
        Assert.assertEquals("Not correct handler state", Handler.CCState.READY, handler.getState());
    }
}
