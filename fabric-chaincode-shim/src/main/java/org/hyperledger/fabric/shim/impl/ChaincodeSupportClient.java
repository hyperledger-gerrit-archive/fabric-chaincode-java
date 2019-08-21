/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc;
import org.hyperledger.fabric.protos.peer.ChaincodeSupportGrpc.ChaincodeSupportStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;

public class ChaincodeSupportClient {
	private static Logger logger = Logger.getLogger(ChaincodeSupportClient.class.getName());
	private static Logger perflogger = Logger.getLogger("Performance");
	
	private final ManagedChannel channel;
	private final ChaincodeSupportStub stub;

	public ChaincodeSupportClient(ManagedChannelBuilder<?> channelBuilder) {
		this.channel = channelBuilder.build();
		this.stub = ChaincodeSupportGrpc.newStub(channel);
	}

	public void shutdown() throws InterruptedException {
		this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	public StreamObserver<ChaincodeMessage> register(StreamObserver<ChaincodeMessage> responseObserver) {
		return stub.register(responseObserver);
	}

	public void start(InnvocationTaskManager itm) {

		// the response stream is the message flow FROM the peer
		// the request observer is the message flow TO the peer

		// route the messages from the peer to the InnvocationTaskManager, to be handled
		// to the
		// correct Task for processing.
		Consumer<ChaincodeMessage> consumer = itm::onChaincodeMessage;

		logger.info("making the grpc call");
		StreamObserver<ChaincodeMessage> requestObserver = this.stub.register(

				new StreamObserver<ChaincodeMessage>() {
					@Override
					public void onNext(ChaincodeMessage chaincodeMessage) {
						consumer.accept(chaincodeMessage);
					}

					@Override
					public void onError(Throwable t) {
						logger.error("An error occured on the chaincode stream. Shutting down the chaincode stream."
								+ logger.formatError(t));

						// ChaincodeSupportStream.this.shutdown();
					}

					@Override
					public void onCompleted() {
						logger.error("Chaincode stream is complete. Shutting down the chaincode stream.");
					}
				}

		);

		// Consumer function for response messages
		Consumer<ChaincodeMessage> c = new Consumer<ChaincodeMessage>() {
			
			// create a lock, with fair property
			ReentrantLock lock = new ReentrantLock(true);

			@Override
			public void accept(ChaincodeMessage t) {
				lock.lock();
				perflogger.fine(()->"> sendToPeer "+t.getTxid());
				requestObserver.onNext(t);
				perflogger.fine(()->"< sendToPeer "+t.getTxid());
				lock.unlock();
			}
		};
		
		// Pass a Consumer interface back to the the task manager. This is for tasks to use to respond back to the peer. 
		itm.setResponseConsumer(c).register();

	}
}
