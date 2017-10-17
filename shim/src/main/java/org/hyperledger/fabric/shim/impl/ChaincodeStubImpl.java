/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult.KV;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage.ChaincodeEvent;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryResultBytes;
import org.hyperledger.fabric.protos.peer.ProposalPackage.SignedProposal;
import org.hyperledger.fabric.shim.Chaincode.Response;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

class ChaincodeStubImpl implements ChaincodeStub {

	private static final String UNSPECIFIED_KEY = new String(Character.toChars(0x000001));
	private final String txId;
	private final Handler handler;
	private final List<ByteString> args;
	private final SignedProposal signedProposal;
	private ChaincodeEvent event;

	ChaincodeStubImpl(String txId, Handler handler, List<ByteString> args, SignedProposal signedProposal) {
		this.txId = txId;
		this.handler = handler;
		this.args = Collections.unmodifiableList(args);
		this.signedProposal = signedProposal;
	}

	@Override
	public List<byte[]> getArgs() {
		return args.stream().map(x -> x.toByteArray()).collect(Collectors.toList());
	}

	@Override
	public List<String> getStringArgs() {
		return args.stream().map(x -> x.toStringUtf8()).collect(Collectors.toList());
	}

	@Override
	public String getFunction() {
		return getStringArgs().size() > 0 ? getStringArgs().get(0) : null;
	}

	@Override
	public List<String> getParameters() {
		return getStringArgs().stream().skip(1).collect(toList());
	}

	@Override
	public void setEvent(String name, byte[] payload) {
		if (name == null || name.trim().length() == 0) throw new IllegalArgumentException("Event name cannot be null or empty string.");
		if (payload != null) {
			this.event = ChaincodeEvent.newBuilder()
					.setEventName(name)
					.setPayload(ByteString.copyFrom(payload))
					.build();
		} else {
			this.event = ChaincodeEvent.newBuilder()
					.setEventName(name)
					.build();
		}
	}

	@Override
	public ChaincodeEvent getEvent() {
		return event;
	}

	@Override
	public String getTxId() {
		return txId;
	}

	@Override
	public byte[] getState(String key) {
		return handler.getState(txId, key).toByteArray();
	}

	@Override
	public void putState(String key, byte[] value) {
		if(key == null) throw new NullPointerException("key cannot be null");
		if(key.length() == 0) throw new IllegalArgumentException("key cannot not be an empty string");
		handler.putState(txId, key, ByteString.copyFrom(value));
	}

	@Override
	public void delState(String key) {
		handler.deleteState(txId, key);
	}

	@Override
	public QueryResultsIterator<KeyValue> getStateByRange(String startKey, String endKey) {
		if (startKey == null || startKey.isEmpty()) startKey = UNSPECIFIED_KEY;
		if (endKey == null || endKey.isEmpty()) endKey = UNSPECIFIED_KEY;
		CompositeKey.validateSimpleKeys(startKey, endKey);

		return new QueryResultsIteratorImpl<KeyValue>(this.handler, getTxId(),
				handler.getStateByRange(getTxId(), startKey, endKey),
				queryResultBytesToKv.andThen(KeyValueImpl::new)
				);
	}

	private Function<QueryResultBytes, KV> queryResultBytesToKv = new Function<QueryResultBytes, KV>() {
		public KV apply(QueryResultBytes queryResultBytes) {
			try {
				return KV.parseFrom(queryResultBytes.getResultBytes());
			} catch (InvalidProtocolBufferException e) {
				throw new RuntimeException(e);
			}
		};
	};

	@Override
	public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String compositeKey) {
		if (compositeKey == null || compositeKey.isEmpty()) {
			compositeKey = UNSPECIFIED_KEY;
		}
		return getStateByRange(compositeKey, compositeKey + "\udbff\udfff");
	}

	@Override
	public CompositeKey createCompositeKey(String objectType, String... attributes) {
		return new CompositeKey(objectType, attributes);
	}

	@Override
	public CompositeKey splitCompositeKey(String compositeKey) {
		return CompositeKey.parseCompositeKey(compositeKey);
	}

	@Override
	public QueryResultsIterator<KeyValue> getQueryResult(String query) {
		return new QueryResultsIteratorImpl<KeyValue>(this.handler, getTxId(),
				handler.getQueryResult(getTxId(), query),
				queryResultBytesToKv.andThen(KeyValueImpl::new)
				);
	}

	@Override
	public QueryResultsIterator<KeyModification> getHistoryForKey(String key) {
		return new QueryResultsIteratorImpl<KeyModification>(this.handler, getTxId(),
				handler.getHistoryForKey(getTxId(), key),
				queryResultBytesToKeyModification.andThen(KeyModificationImpl::new)
				);
	}

	private Function<QueryResultBytes, KvQueryResult.KeyModification> queryResultBytesToKeyModification = new Function<QueryResultBytes, KvQueryResult.KeyModification>() {
		public KvQueryResult.KeyModification apply(QueryResultBytes queryResultBytes) {
			try {
				return KvQueryResult.KeyModification.parseFrom(queryResultBytes.getResultBytes());
			} catch (InvalidProtocolBufferException e) {
				throw new RuntimeException(e);
			}
		};
	};

	@Override
	public Response invokeChaincode(final String chaincodeName, final List<byte[]> args, final String channel) {
		// internally we handle chaincode name as a composite name
		final String compositeName;
		if (channel != null && channel.trim().length() > 0) {
			compositeName = chaincodeName + "/" + channel;
		} else {
			compositeName = chaincodeName;
		}
		return handler.invokeChaincode(this.txId, compositeName, args);
	}

	@Override
	public SignedProposal getSignedProposal() {
		return signedProposal;
	}
}
