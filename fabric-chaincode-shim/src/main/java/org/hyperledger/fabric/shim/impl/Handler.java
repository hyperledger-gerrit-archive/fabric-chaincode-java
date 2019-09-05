/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim.impl;

import static java.lang.String.format;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.COMPLETED;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.DEL_STATE;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.ERROR;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.GET_PRIVATE_DATA_HASH;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.GET_QUERY_RESULT;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.GET_STATE;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.GET_STATE_BY_RANGE;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.GET_STATE_METADATA;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.INVOKE_CHAINCODE;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.KEEPALIVE;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.PUT_STATE;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.PUT_STATE_METADATA;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.QUERY_STATE_CLOSE;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.QUERY_STATE_NEXT;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.READY;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.REGISTER;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.REGISTERED;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.RESPONSE;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInput;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage.ChaincodeEvent;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.DelState;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.GetQueryResult;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.GetState;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.GetStateByRange;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.GetStateMetadata;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.PutState;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.PutStateMetadata;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryResponse;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryStateClose;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryStateNext;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.StateMetadata;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.StateMetadataResult;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage.Response;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage.Response.Builder;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.helper.Channel;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

public class Handler {

    private static Logger logger = Logger.getLogger(Handler.class.getName());
    private final Chaincode chaincode;
    private final Map<String, Channel<ChaincodeMessage>> responseChannel = new ConcurrentHashMap<>();
    private Channel<ChaincodeMessage> outboundChaincodeMessages = new Channel<>("outboundChaincodeMessages");
    private CCState state;

    public Handler(ChaincodeID chaincodeId, Chaincode chaincode) {
        this.chaincode = chaincode;
        this.state = CCState.CREATED;
        queueOutboundChaincodeMessage(ChaincodeMessageFactory.newRegisterChaincodeMessage(chaincodeId));
    }

    public ChaincodeMessage nextOutboundChaincodeMessage() {
        try {
            return outboundChaincodeMessages.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("Unable to get next outbound ChaincodeMessage");
            }
            return ChaincodeMessageFactory.newErrorEventMessage("UNKNOWN", "UNKNOWN", e);
        }
    }

    public void onChaincodeMessage(ChaincodeMessage chaincodeMessage) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(format("[%-8.8s] %s", chaincodeMessage.getTxid(), toJsonString(chaincodeMessage)));
        }
        handleChaincodeMessage(chaincodeMessage);
    }

    private void handleChaincodeMessage(ChaincodeMessage message) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(format("[%-8.8s] Handling ChaincodeMessage of type: %s, handler state %s", message.getTxid(), message.getType(), this.state));
        }
        if (message.getType() == KEEPALIVE) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(format("[%-8.8s] Received KEEPALIVE: nothing to do", message.getTxid()));
            }
            return;
        }
        switch (this.state) {
            case CREATED:
                handleCreated(message);
                break;
            case ESTABLISHED:
                handleEstablished(message);
                break;
            case READY:
                handleReady(message);
                break;
            default:
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning(format("[%-8.8s] Received %s: cannot handle", message.getTxid(), message.getType()));
                }
                break;
        }
    }

    private void handleCreated(ChaincodeMessage message) {
        if (message.getType() == REGISTERED) {
            this.state = CCState.ESTABLISHED;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(format("[%-8.8s] Received REGISTERED: moving to established state", message.getTxid()));
            }
        } else {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning(format("[%-8.8s] Received %s: cannot handle", message.getTxid(), message.getType()));
            }
        }
    }

    private void handleEstablished(ChaincodeMessage message) {
        if (message.getType() == READY) {
            this.state = CCState.READY;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(format("[%-8.8s] Received READY: ready for invocations", message.getTxid()));
            }
        } else {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning(format("[%-8.8s] Received %s: cannot handle", message.getTxid(), message.getType()));
            }
        }
    }

    private void handleReady(ChaincodeMessage message) {
        switch (message.getType()) {
            case RESPONSE:
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(format("[%-8.8s] Received RESPONSE: publishing to channel", message.getTxid()));
                }
                sendChannel(message);
                break;
            case ERROR:
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(format("[%-8.8s] Received ERROR: publishing to channel", message.getTxid()));
                }
                sendChannel(message);
                break;
            case INIT:
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(format("[%-8.8s] Received INIT: invoking chaincode init", message.getTxid()));
                }
                handleInit(message);
                break;
            case TRANSACTION:
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(format("[%-8.8s] Received TRANSACTION: invoking chaincode", message.getTxid()));
                }
                handleTransaction(message);
                break;
            default:
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning(format("[%-8.8s] Received %s: cannot handle", message.getTxid(), message.getType()));
                }
                break;
        }
    }

    private String getTxKey(final String channelId, final String txid) {
        return channelId + txid;
    }

    private void queueOutboundChaincodeMessage(ChaincodeMessage chaincodeMessage) {
        this.outboundChaincodeMessages.add(chaincodeMessage);
    }

    private Channel<ChaincodeMessage> aquireResponseChannelForTx(final String channelId, final String txId) {
    	String key = getTxKey(channelId, txId);
        final Channel<ChaincodeMessage> channel = new Channel<>();
        
        if (this.responseChannel.putIfAbsent(key, channel) != null) {
            throw new IllegalStateException(format("[%-8.8s] Response channel already exists. Another request must be pending.", txId));
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("[%-8.8s] Response channel created.", txId));
        }
        return channel;
    }

    private void sendChannel(ChaincodeMessage message) {
        String key = getTxKey(message.getChannelId(), message.getTxid());
        if (!responseChannel.containsKey(key)) {
            throw new IllegalStateException(format("[%-8.8s] sendChannel does not exist", message.getTxid()));
        }
        responseChannel.get(key).add(message);
    }

    private ChaincodeMessage receiveChannel(Channel<ChaincodeMessage> channel) {
        try {
            return channel.take();
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("channel.take() failed with InterruptedException");
            }

            // Channel has been closed?
            // TODO
            return null;
        }
    }

    private void releaseResponseChannelForTx(String channelId, String txId) {
        String key = getTxKey(channelId, txId);
        final Channel<ChaincodeMessage> channel = responseChannel.remove(key);
        if (channel != null) channel.close();
        if (logger.isLoggable(Level.FINER)) {
            logger.finer(format("[%-8.8s] Response channel closed.", txId));
        }
    }

    /**
     * Handles requests to initialize chaincode
     *
     * @param message chaincode to be initialized
     */
    private void handleInit(ChaincodeMessage message) {
        new Thread(() -> {
            try {

                // Get the function and args from Payload
                final ChaincodeInput input = ChaincodeInput.parseFrom(message.getPayload());

                // Create the ChaincodeStub which the chaincode can use to
                // callback
                final ChaincodeStub stub = new ChaincodeStubImpl(message.getChannelId(), message.getTxid(), this, input.getArgsList(), message.getProposal());

                // Call chaincode's init
                final Chaincode.Response result = chaincode.init(stub);

                if (result.getStatus().getCode() >= Chaincode.Response.Status.INTERNAL_SERVER_ERROR.getCode()) {
                    // Send ERROR with entire result.Message as payload
                    logger.severe(format("[%-8.8s] Init failed. Sending %s", message.getTxid(), ERROR));
                    queueOutboundChaincodeMessage(ChaincodeMessageFactory.newErrorEventMessage(message.getChannelId(), message.getTxid(), result.getMessage(), stub.getEvent()));
                } else {
                    // Send COMPLETED with entire result as payload
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(format(format("[%-8.8s] Init succeeded. Sending %s", message.getTxid(), COMPLETED)));
                    }
                    queueOutboundChaincodeMessage(ChaincodeMessageFactory.newCompletedEventMessage(message.getChannelId(), message.getTxid(), result, stub.getEvent()));
                }
            } catch (InvalidProtocolBufferException | RuntimeException e) {
                logger.severe(format("[%-8.8s] Init failed. Sending %s: %s", message.getTxid(), ERROR, e));
                queueOutboundChaincodeMessage(ChaincodeMessageFactory.newErrorEventMessage(message.getChannelId(), message.getTxid(), e));
            }
        }).start();
    }

    // handleTransaction Handles request to execute a transaction.
    private void handleTransaction(ChaincodeMessage message) {
        new Thread(() -> {
            try {

                // Get the function and args from Payload
                final ChaincodeInput input = ChaincodeInput.parseFrom(message.getPayload());

                // Create the ChaincodeStub which the chaincode can use to
                // callback
                final ChaincodeStub stub = new ChaincodeStubImpl(message.getChannelId(), message.getTxid(), this, input.getArgsList(), message.getProposal());

                // Call chaincode's invoke
                final Chaincode.Response result = chaincode.invoke(stub);

                if (result.getStatus().getCode() >= Chaincode.Response.Status.INTERNAL_SERVER_ERROR.getCode()) {
                    // Send ERROR with entire result.Message as payload
                    logger.severe(format("[%-8.8s] Invoke failed. Sending %s", message.getTxid(), ERROR));
                    queueOutboundChaincodeMessage(ChaincodeMessageFactory.newErrorEventMessage(message.getChannelId(), message.getTxid(), result.getMessage(), stub.getEvent()));
                } else {
                    // Send COMPLETED with entire result as payload
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(format(format("[%-8.8s] Invoke succeeded. Sending %s", message.getTxid(), COMPLETED)));
                    }
                    queueOutboundChaincodeMessage(ChaincodeMessageFactory.newCompletedEventMessage(message.getChannelId(), message.getTxid(), result, stub.getEvent()));
                }

            } catch (InvalidProtocolBufferException | RuntimeException e) {
                logger.severe(format("[%-8.8s] Invoke failed. Sending %s: %s", message.getTxid(), ERROR, e));
                queueOutboundChaincodeMessage(ChaincodeMessageFactory.newErrorEventMessage(message.getChannelId(), message.getTxid(), e));
            }
        }).start();
    }

    // handleGetState communicates with the validator to fetch the requested state information from the ledger.
    ByteString getState(String channelId, String txId, String collection, String key) {
        return invokeChaincodeSupport(ChaincodeMessageFactory.newGetStateEventMessage(channelId, txId, collection, key));
    }

    ByteString getPrivateDataHash(String channelId, String txId, String collection, String key) {
        return invokeChaincodeSupport(ChaincodeMessageFactory.newGetPrivateDataHashEventMessage(channelId, txId, collection, key));
    }

    Map<String, ByteString> getStateMetadata(String channelId, String txId, String collection, String key) {
        ByteString payload = invokeChaincodeSupport(ChaincodeMessageFactory.newGetStateMetadataEventMessage(channelId, txId, collection, key));
        try {
            StateMetadataResult stateMetadataResult = StateMetadataResult.parseFrom(payload);
            Map<String, ByteString> stateMetadataMap = new HashMap<>();
            stateMetadataResult.getEntriesList().forEach(entry -> stateMetadataMap.put(entry.getMetakey(), entry.getValue()));
            return stateMetadataMap;
        } catch (InvalidProtocolBufferException e) {
            logger.severe(String.format("[%-8.8s] unmarshall error", txId));
            throw new RuntimeException("Error unmarshalling StateMetadataResult.", e);
        }
    }

     void putState(String channelId, String txId, String collection, String key, ByteString value) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(format("[%-8.8s] Inside putstate (\"%s\":\"%s\":\"%s\")", txId, collection, key, value));
        }
        invokeChaincodeSupport(ChaincodeMessageFactory.newPutStateEventMessage(channelId, txId, collection, key, value));
    }

    void putStateMetadata(String channelId, String txId, String collection, String key, String metakey, ByteString value) {
        invokeChaincodeSupport(ChaincodeMessageFactory.newPutStateMatadateEventMessage(channelId, txId, collection, key, metakey, value));
    }

    void deleteState(String channelId, String txId, String collection, String key) {
        invokeChaincodeSupport(ChaincodeMessageFactory.newDeleteStateEventMessage(channelId, txId, collection, key));
    }

    QueryResponse getStateByRange(String channelId, String txId, String collection, String startKey, String endKey, ByteString metadata) {
        GetStateByRange.Builder msgBuilder = GetStateByRange.newBuilder()
            .setCollection(collection)
            .setStartKey(startKey)
            .setEndKey(endKey);
        if (metadata != null) {
            msgBuilder.setMetadata(metadata);
        }
        return invokeQueryResponseMessage(channelId, txId, GET_STATE_BY_RANGE, msgBuilder.build().toByteString());
    }

    QueryResponse queryStateNext(String channelId, String txId, String queryId) {
        return invokeQueryResponseMessage(channelId, txId, QUERY_STATE_NEXT, QueryStateNext.newBuilder()
                .setId(queryId)
                .build().toByteString());
    }

    void queryStateClose(String channelId, String txId, String queryId) {
        invokeQueryResponseMessage(channelId, txId, QUERY_STATE_CLOSE, QueryStateClose.newBuilder()
                .setId(queryId)
                .build().toByteString());
    }

    QueryResponse getQueryResult(String channelId, String txId, String collection, String query, ByteString metadata) {
        GetQueryResult.Builder msgBuilder = GetQueryResult.newBuilder()
                .setCollection(collection)
                .setQuery(query);
        if (metadata != null) {
            msgBuilder.setMetadata(metadata);
        }
        return invokeQueryResponseMessage(channelId, txId, GET_QUERY_RESULT, msgBuilder.build().toByteString());
    }

    QueryResponse getHistoryForKey(String channelId, String txId, String key) {
        return invokeQueryResponseMessage(channelId, txId, Type.GET_HISTORY_FOR_KEY, GetQueryResult.newBuilder()
                .setQuery(key)
                .build().toByteString());
    }

    private QueryResponse invokeQueryResponseMessage(String channelId, String txId, ChaincodeMessage.Type type, ByteString payload) {
        try {
            return QueryResponse.parseFrom(invokeChaincodeSupport(ChaincodeMessageFactory.newEventMessage(type, channelId, txId, payload)));
        } catch (InvalidProtocolBufferException e) {
            logger.severe(String.format("[%-8.8s] unmarshall error", txId));
            throw new RuntimeException("Error unmarshalling QueryResponse.", e);
        }
    }

    protected ByteString invokeChaincodeSupport(final ChaincodeMessage message) {
        final String channelId = message.getChannelId();
        final String txId = message.getTxid();

        try {
            // create a new response channel
            Channel<ChaincodeMessage> responseChannel = aquireResponseChannelForTx(channelId, txId);

            // send the message
            queueOutboundChaincodeMessage(message);

            // wait for response
            final ChaincodeMessage response = receiveChannel(responseChannel);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(format("[%-8.8s] %s response received.", txId, response.getType()));
            }

            // handle response
            switch (response.getType()) {
                case RESPONSE:
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(format("[%-8.8s] Successful response received.", txId));
                    }
                    return response.getPayload();
                case ERROR:
                    logger.severe(format("[%-8.8s] Unsuccessful response received.", txId));
                    throw new RuntimeException(format("[%-8.8s]Unsuccessful response received.", txId));
                default:
                    logger.severe(format("[%-8.8s] Unexpected %s response received. Expected %s or %s.", txId, response.getType(), RESPONSE, ERROR));
                    throw new RuntimeException(format("[%-8.8s]Unexpected %s response received. Expected %s or %s.", txId, response.getType(), RESPONSE, ERROR));
            }
        } finally {
            releaseResponseChannelForTx(channelId, txId);
        }
    }

    Chaincode.Response invokeChaincode(String channelId, String txId, String chaincodeName, List<byte[]> args) {
        try {
            // create invocation specification of the chaincode to invoke
            final ChaincodeSpec invocationSpec = ChaincodeSpec.newBuilder()
                    .setChaincodeId(ChaincodeID.newBuilder()
                            .setName(chaincodeName)
                            .build())
                    .setInput(ChaincodeInput.newBuilder()
                            .addAllArgs(args.stream().map(ByteString::copyFrom).collect(Collectors.toList()))
                            .build())
                    .build();

            // invoke other chaincode
            final ByteString payload = invokeChaincodeSupport(ChaincodeMessageFactory.newInvokeChaincodeMessage(channelId, txId, invocationSpec.toByteString()));

            // response message payload should be yet another chaincode
            // message (the actual response message)
            final ChaincodeMessage responseMessage = ChaincodeMessage.parseFrom(payload);
            // the actual response message must be of type COMPLETED
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(format("[%-8.8s] %s response received from other chaincode.", txId, responseMessage.getType()));
            }
            if (responseMessage.getType() == COMPLETED) {
                // success
                return toChaincodeResponse(Response.parseFrom(responseMessage.getPayload()));
            } else {
                // error
                return newErrorChaincodeResponse(responseMessage.getPayload().toStringUtf8());
            }
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toJsonString(ChaincodeMessage message) {
        try {
            return JsonFormat.printer().print(message);
        } catch (InvalidProtocolBufferException e) {
            return String.format("{ Type: %s, TxId: %s }", message.getType(), message.getTxid());
        }
    }

    private static Chaincode.Response newErrorChaincodeResponse(String message) {
        return new Chaincode.Response(Chaincode.Response.Status.INTERNAL_SERVER_ERROR, message, null);
    }



    private static Chaincode.Response toChaincodeResponse(Response response) {
        return new Chaincode.Response(
                Chaincode.Response.Status.forCode(response.getStatus()),
                response.getMessage(),
                response.getPayload() == null ? null : response.getPayload().toByteArray()
        );
    }



    public enum CCState {
        CREATED,
        ESTABLISHED,
        READY
    }

    CCState getState() {
        return this.state;
    }

}
