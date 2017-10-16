/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.hamcrest.Matchers;
import org.hyperledger.fabric.protos.common.Common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Common.Header;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult.KV;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage.ChaincodeEvent;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryResponse;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryResultBytes;
import org.hyperledger.fabric.protos.peer.ProposalPackage.Proposal;
import org.hyperledger.fabric.protos.peer.ProposalPackage.SignedProposal;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.Chaincode.Response.Status;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hyperledger.fabric.protos.common.Common.HeaderType.ENDORSER_TRANSACTION_VALUE;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChaincodeStubImplTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Mock
	private Handler handler;

	@Test
	public void testGetArgs() {
		List<ByteString> args = Arrays.asList(
				ByteString.copyFromUtf8("arg0"),
				ByteString.copyFromUtf8("arg1"),
				ByteString.copyFromUtf8("arg2"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, args, null);
		assertThat(stub.getArgs(), contains(args.stream().map(ByteString::toByteArray).toArray()));
	}

	@Test
	public void testGetStringArgs() {
		List<ByteString> args = Arrays.asList(
				ByteString.copyFromUtf8("arg0"),
				ByteString.copyFromUtf8("arg1"),
				ByteString.copyFromUtf8("arg2"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, args, null);
		assertThat(stub.getStringArgs(), contains(args.stream().map(ByteString::toStringUtf8).toArray()));
	}

	@Test
	public void testGetFunction() {
		List<ByteString> args = Arrays.asList(
				ByteString.copyFromUtf8("function"),
				ByteString.copyFromUtf8("arg0"),
				ByteString.copyFromUtf8("arg1"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, args, null);
		assertThat(stub.getFunction(), is("function"));
	}

	@Test
	public void testGetParameters() {
		List<ByteString> args = Arrays.asList(
				ByteString.copyFromUtf8("function"),
				ByteString.copyFromUtf8("arg0"),
				ByteString.copyFromUtf8("arg1"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, args, null);
		assertThat(stub.getParameters(), contains("arg0", "arg1"));
	}

	@Test
	public void testSetGetEvent() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final byte[] payload = new byte[]{0x10, 0x20, 0x20};
		final String eventName = "event_name";
		stub.setEvent(eventName, payload);
		ChaincodeEvent event = stub.getEvent();
		assertThat(event, hasProperty("eventName", equalTo(eventName)));
		assertThat(event, hasProperty("payload", equalTo(ByteString.copyFrom(payload))));

		stub.setEvent(eventName, null);
		event = stub.getEvent();
		assertNotNull(event);
		assertThat(event, hasProperty("eventName", equalTo(eventName)));
		assertThat(event, hasProperty("payload", equalTo(ByteString.copyFrom(new byte[0]))));
	}

	@Test
	public void testSetEventEmptyName() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		thrown.expect(Matchers.isA(IllegalArgumentException.class));
		stub.setEvent("", new byte[0]);
	}

	@Test
	public void testSetEventNullName() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		thrown.expect(Matchers.isA(IllegalArgumentException.class));
		stub.setEvent(null, new byte[0]);
	}

	@Test
	public void testGetTxId() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		assertThat(stub.getTxId(), is("txId"));
	}

	@Test
	public void testGetState() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final byte[] value = new byte[]{0x10, 0x20, 0x30};
		when(handler.getState("txId", "key")).thenReturn(ByteString.copyFrom(value));
		assertThat(stub.getState("key"), is(value));
	}

	@Test
	public void testGetStringState() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final String value = "TEST";
		when(handler.getState("txId", "key")).thenReturn(ByteString.copyFromUtf8(value));
		assertThat(stub.getStringState("key"), is(value));
	}

	@Test
	public void testPutState() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final byte[] value = new byte[]{0x10, 0x20, 0x30};
		stub.putState("key", value);
		verify(handler).putState("txId", "key", ByteString.copyFrom(value));
	}

	@Test
	public void testStringState() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final String value = "TEST";
		stub.putStringState("key", value);
		verify(handler).putState("txId", "key", ByteString.copyFromUtf8(value));
	}

	@Test
	public void testDelState() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		stub.delState("key");
		verify(handler).deleteState("txId", "key");
	}

	@Test
	public void testGetStateByRange() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final String startKey = "START";
		final String endKey = "END";
		final KV[] keyValues = new KV[]{
				KV.newBuilder()
						.setKey("A")
						.setValue(ByteString.copyFromUtf8("Value of A"))
						.build(),
				KV.newBuilder()
						.setKey("B")
						.setValue(ByteString.copyFromUtf8("Value of B"))
						.build()
		};
		final QueryResponse value = QueryResponse.newBuilder()
				.setHasMore(false)
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyValues[0].toByteString()))
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyValues[1].toByteString()))
				.build();
		when(handler.getStateByRange("txId", startKey, endKey)).thenReturn(value);
		assertThat(stub.getStateByRange(startKey, endKey), contains(Arrays.stream(keyValues).map(KeyValueImpl::new).toArray()));
	}

	@Test
	public void testGetStateByPartialCompositeKey() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final KV[] keyValues = new KV[]{
				KV.newBuilder()
						.setKey("A")
						.setValue(ByteString.copyFromUtf8("Value of A"))
						.build(),
				KV.newBuilder()
						.setKey("B")
						.setValue(ByteString.copyFromUtf8("Value of B"))
						.build()
		};
		final QueryResponse value = QueryResponse.newBuilder()
				.setHasMore(false)
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyValues[0].toByteString()))
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyValues[1].toByteString()))
				.build();
		when(handler.getStateByRange(anyString(), anyString(), anyString())).thenReturn(value);
		stub.getStateByPartialCompositeKey(null);
		verify(handler).getStateByRange("txId", "\u0001", "\u0001\udbff\udfff");
	}

	@Test
	public void testCreateCompositeKey() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final CompositeKey key = stub.createCompositeKey("abc", "def", "ghi", "jkl", "mno");
		assertThat(key, hasProperty("objectType", equalTo("abc")));
		assertThat(key, hasProperty("attributes", hasSize(4)));
		assertThat(key, Matchers.hasToString(equalTo("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test
	public void testSplitCompositeKey() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final CompositeKey key = stub.splitCompositeKey("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000");
		assertThat(key, hasProperty("objectType", equalTo("abc")));
		assertThat(key, hasProperty("attributes", contains("def", "ghi", "jkl", "mno")));
		assertThat(key, Matchers.hasToString(equalTo("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test
	public void testGetQueryResult() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final KV[] keyValues = new KV[]{
				KV.newBuilder()
						.setKey("A")
						.setValue(ByteString.copyFromUtf8("Value of A"))
						.build(),
				KV.newBuilder()
						.setKey("B")
						.setValue(ByteString.copyFromUtf8("Value of B"))
						.build()
		};
		final QueryResponse value = QueryResponse.newBuilder()
				.setHasMore(false)
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyValues[0].toByteString()))
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyValues[1].toByteString()))
				.build();
		when(handler.getQueryResult("txId", "QUERY")).thenReturn(value);
		assertThat(stub.getQueryResult("QUERY"), contains(Arrays.stream(keyValues).map(KeyValueImpl::new).toArray()));
	}

	@Test(expected = InvalidProtocolBufferException.class)
	public void testGetQueryResultWithException() throws Throwable {
		final String txId = "txId", query = "QUERY";
		final ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, handler, Collections.emptyList(), null);
		final QueryResponse value = QueryResponse.newBuilder()
				.setHasMore(false)
				.addResults(QueryResultBytes.newBuilder().setResultBytes(ByteString.copyFromUtf8("exception")))
				.build();
		when(handler.getQueryResult(txId, query)).thenReturn(value);
		try {
			stub.getQueryResult(query).iterator().next();
		} catch (RuntimeException e) {
			throw e.getCause();
		}
	}

	@Test
	public void testGetHistoryForKey() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txId", handler, Collections.emptyList(), null);
		final KvQueryResult.KeyModification[] keyModifications = new KvQueryResult.KeyModification[]{
				KvQueryResult.KeyModification.newBuilder()
						.setTxId("tx0")
						.setTimestamp(Timestamp.getDefaultInstance())
						.setValue(ByteString.copyFromUtf8("Value A"))
						.build(),
				KvQueryResult.KeyModification.newBuilder()
						.setTxId("tx1")
						.setTimestamp(Timestamp.getDefaultInstance())
						.setValue(ByteString.copyFromUtf8("Value B"))
						.build()
		};
		final QueryResponse value = QueryResponse.newBuilder()
				.setHasMore(false)
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyModifications[0].toByteString()))
				.addResults(QueryResultBytes.newBuilder().setResultBytes(keyModifications[1].toByteString()))
				.build();
		when(handler.getHistoryForKey("txId", "KEY")).thenReturn(value);
		assertThat(stub.getHistoryForKey("KEY"), contains(Arrays.stream(keyModifications).map(KeyModificationImpl::new).toArray()));
	}

	@Test(expected = InvalidProtocolBufferException.class)
	public void testGetHistoryForKeyWithException() throws Throwable {
		final String txId = "txId", key = "KEY";
		final ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, handler, Collections.emptyList(), null);
		final QueryResponse value = QueryResponse.newBuilder()
				.setHasMore(false)
				.addResults(QueryResultBytes.newBuilder().setResultBytes(ByteString.copyFromUtf8("exception")))
				.build();
		when(handler.getHistoryForKey(txId, key)).thenReturn(value);
		try {
			stub.getHistoryForKey(key).iterator().next();
		} catch (RuntimeException e) {
			throw e.getCause();
		}
	}

	@Test
	public void testInvokeChaincode() {
		final String txId = "txId", chaincodeName = "CHAINCODE_ID", channel = "CHAINCODE_CHANNEL";
		final ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, handler, Collections.emptyList(), null);
		final Chaincode.Response expectedResponse = new Chaincode.Response(Status.SUCCESS, "MESSAGE", "PAYLOAD".getBytes(UTF_8));
		when(handler.invokeChaincode(txId, chaincodeName, Collections.emptyList())).thenReturn(expectedResponse);
		assertThat(stub.invokeChaincode(chaincodeName, Collections.emptyList()), is(expectedResponse));

		when(handler.invokeChaincode(eq(txId), eq(chaincodeName + "/" + channel), anyList())).thenReturn(expectedResponse);
		assertThat(stub.invokeChaincode(chaincodeName, Collections.emptyList(), channel), is(expectedResponse));
	}

	@Test
	public void testGetSignedProposal() {
		final SignedProposal signedProposal = SignedProposal.newBuilder()
				.setProposalBytes(Proposal.newBuilder()
						.setHeader(Header.newBuilder()
								.setChannelHeader(ChannelHeader.newBuilder()
										.setType(ENDORSER_TRANSACTION_VALUE)
										.setTimestamp(Timestamp.getDefaultInstance())
										.build().toByteString()
								)
								.build().toByteString()
						)
						.build().toByteString()
				).build();
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>(), signedProposal);
		assertThat(stub.getSignedProposal(), is(signedProposal));
	}
}