/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.impl;

import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.COMPLETED;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.ERROR;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.RESPONSE;

import java.util.concurrent.Callable;
import java.util.concurrent.Exchanger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.hyperledger.fabric.Logging;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeStub;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * A Callable implementation the has the job of invoking the chaincode, and
 * matching the response and requests
 *
 */
public class ChaincodeInnvocationTask implements Callable<ChaincodeMessage> {

    private static Logger logger = Logging.getLogger(ChaincodeInnvocationTask.class);
    private static Logger perflogger = Logger.getLogger("org.hyperledger.Performance");

    private String key;
    private Type type;
    private String txId;
    private Consumer<ChaincodeMessage> outgoingMessageConsumer;
    private Exchanger<ChaincodeMessage> messageExchange = new Exchanger<>();
    private ChaincodeMessage message;
    private Chaincode chaincode;

    /**
     *
     * @param message         The incoming message that has triggered this task into
     *                        execution
     * @param type            Is this init or invoke? (v2 Fabric deprecates init)
     * @param outgoingMessage The Consumer functional interface to send any requests
     *                        for ledger state
     * @param chaincode       A instance of the end users chaincode
     *
     */
    public ChaincodeInnvocationTask(ChaincodeMessage message, Type type, Consumer<ChaincodeMessage> outgoingMessage,
            Chaincode chaincode) {

        this.key = message.getChannelId() + message.getTxid();
        this.type = type;
        this.outgoingMessageConsumer = outgoingMessage;
        this.txId = message.getTxid();
        this.chaincode = chaincode;
        this.message = message;
    }

    /**
     * Main method to power the invocation of the chaincode;
     *
     *
     */
    @Override
    public ChaincodeMessage call() {
        ChaincodeMessage finalResponseMessage;

        try {
            perflogger.fine(() -> "> taskStart " + this.txId);

            // Stub to handle the callbacks for the chaincode to the peer
            ChaincodeStub stub = new InnvocationStubImpl(message,
                    ChaincodeInnvocationTask.this::invokeChaincodeSupport);

            // result is what will be sent to the peer as a response to this invocation
            final Chaincode.Response result;

            // Call chaincode's invoke
            // Note in Fabric v2, there won't be any INIT
            if (this.type.equals(Type.INIT)) {
                result = chaincode.init(stub);
            } else {
                result = chaincode.invoke(stub);
            }

            if (result.getStatus().getCode() >= Chaincode.Response.Status.INTERNAL_SERVER_ERROR.getCode()) {
                // Send ERROR with entire result.Message as payload
                logger.severe(() -> String.format("[%-8.8s] Invoke failed with error code %d. Sending %s", message.getTxid(),result.getStatus().getCode(), ERROR));
                finalResponseMessage = ChaincodeMessageFactory.newErrorEventMessage(message.getChannelId(),
                        message.getTxid(), result.getMessage(), stub.getEvent());
            } else {
                // Send COMPLETED with entire result as payload
                logger.fine(() -> String.format("[%-8.8s] Invoke succeeded. Sending %s", message.getTxid(), COMPLETED));
                finalResponseMessage = ChaincodeMessageFactory.newCompletedEventMessage(message.getChannelId(),
                        message.getTxid(), result, stub.getEvent());
            }

        } catch (InvalidProtocolBufferException | RuntimeException e) {
            logger.severe(() -> String.format("[%-8.8s] Invoke failed. Sending %s: %s", message.getTxid(), ERROR, e));
            finalResponseMessage = ChaincodeMessageFactory.newErrorEventMessage(message.getChannelId(),
                    message.getTxid(), e);
        }

        // send the final response message to the peer
        outgoingMessageConsumer.accept(finalResponseMessage);

        // also return for reference
        return finalResponseMessage;
    }

    /**
     * Identifier of this task, channel id and transaction id
     */
    public String getTxKey() {
        return this.key;
    }

    /**
     * Use the Key as to determine equality
     *
     * @param task
     * @return
     */
    public boolean equals(ChaincodeInnvocationTask task) {
        return key.equals(task.getTxKey());
    }

    /**
     * Posts the message that the peer has responded with to this task's request
     * Uses an 'Exhanger' concurrent lock; this lets two threads exchange values
     * atomically.
     *
     * In this case the data is only passed to the executing tasks.
     *
     * @param msg Chaincode message to pass pack
     * @throws InterruptedException should something really really go wrong
     */
    public void postMessage(ChaincodeMessage msg) throws InterruptedException {
        messageExchange.exchange(msg);
    }

    @FunctionalInterface
    public interface InvokeChaincodeSupport {
        ByteString invoke(ChaincodeMessage message);
    }

    /**
     * Send the chaincode message back to the peer.
     *
     * It will send the message, via the outgoingMessageConsumer, and then block on
     * the 'Exchanger' to wait for the response to come.
     *
     * This Exchange is an atomic operation between the thread that is running this
     * task, and the thread that is handling the communication from the peer.
     *
     * @param message The chaincode message from the peer
     * @return ByteString to be parsed by the caller
     *
     */
    private ByteString invokeChaincodeSupport(final ChaincodeMessage message) {

        // send the message
        logger.info(() -> "Sending message to the peer " + message.getTxid());
        outgoingMessageConsumer.accept(message);

        // wait for response
        ChaincodeMessage response;
        try {
            response = messageExchange.exchange(null);
            logger.info(() -> "Got response back from the peer" + response);
        } catch (InterruptedException e) {
            logger.severe(() -> "Interuptted exchaning messages ");
            throw new RuntimeException(String.format("[%-8.8s]InterruptedException received.", txId), e);
        }
        logger.fine(() -> String.format("[%-8.8s] %s response received.", txId, response.getType()));

        // handle response
        switch (response.getType()) {
        case RESPONSE:
            logger.fine(() -> String.format("[%-8.8s] Successful response received.", txId));
            return response.getPayload();
        case ERROR:
            logger.severe(() -> String.format("[%-8.8s] Unsuccessful response received.", txId));
            throw new RuntimeException(String.format("[%-8.8s]Unsuccessful response received.", txId));
        default:
            logger.severe(() -> String.format("[%-8.8s] Unexpected %s response received. Expected %s or %s.", txId,
                    response.getType(), RESPONSE, ERROR));
            throw new RuntimeException(String.format("[%-8.8s] Unexpected %s response received. Expected %s or %s.",
                    txId, response.getType(), RESPONSE, ERROR));
        }

    }

}
