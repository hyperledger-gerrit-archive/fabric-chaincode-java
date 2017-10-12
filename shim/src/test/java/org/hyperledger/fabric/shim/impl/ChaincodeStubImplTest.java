/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult.KV;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage.ChaincodeEvent;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryResponse;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.QueryResultBytes;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.Chaincode.Response.Status;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

public class ChaincodeStubImplTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testGetArgs() {
		List<ByteString> args = new ArrayList<>();
		args.add(ByteString.copyFromUtf8("arg0"));
		args.add(ByteString.copyFromUtf8("arg1"));
		args.add(ByteString.copyFromUtf8("arg2"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, args);
		assertThat(stub.getArgs(), contains(args.stream().map(ByteString::toByteArray).toArray()));
	}

	@Test
	public void testGetStringArgs() {
		List<ByteString> args = new ArrayList<>();
		args.add(ByteString.copyFromUtf8("arg0"));
		args.add(ByteString.copyFromUtf8("arg1"));
		args.add(ByteString.copyFromUtf8("arg2"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, args);
		assertThat(stub.getStringArgs(), contains(args.stream().map(ByteString::toStringUtf8).toArray()));
	}

	@Test
	public void testGetFunction() {
		List<ByteString> args = new ArrayList<>();
		args.add(ByteString.copyFromUtf8("function"));
		args.add(ByteString.copyFromUtf8("arg0"));
		args.add(ByteString.copyFromUtf8("arg1"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, args);
		assertThat(stub.getFunction(), is("function"));
	}

	@Test
	public void testGetParameters() {
		List<ByteString> args = new ArrayList<>();
		args.add(ByteString.copyFromUtf8("function"));
		args.add(ByteString.copyFromUtf8("arg0"));
		args.add(ByteString.copyFromUtf8("arg1"));
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, args);
		assertThat(stub.getParameters(), contains("arg0", "arg1"));
	}

	@Test
	public void testSetGetEvent() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, new ArrayList<>());
		final byte[] payload = new byte[] { 0x10, 0x20, 0x20 };
		stub.setEvent("event_name", payload);
		final ChaincodeEvent event = stub.getEvent();
		assertThat(event, hasProperty("eventName", equalTo("event_name")));
		assertThat(event, hasProperty("payload", equalTo(ByteString.copyFrom(payload))));
	}

	@Test
	public void testSetEventEmptyName() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, new ArrayList<>());
		thrown.expect(Matchers.isA(IllegalArgumentException.class));
		stub.setEvent("", new byte[0]);
	}

	@Test
	public void testSetEventNullName() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, new ArrayList<>());
		thrown.expect(Matchers.isA(IllegalArgumentException.class));
		stub.setEvent(null, new byte[0]);
	}

	@Test
	public void testGetTxId() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, new ArrayList<>());
		assertThat(stub.getTxId(), is("txid"));
	}

	@Test
	public void testGetState() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final byte[] value = new byte[] { 0x10, 0x20, 0x30 };
		when(handler.getState("txid", "key")).thenReturn(ByteString.copyFrom(value));
		assertThat(stub.getState("key"), is(value));
	}

	@Test
	public void testGetStringState() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final String value = "TEST";
		when(handler.getState("txid", "key")).thenReturn(ByteString.copyFromUtf8(value));
		assertThat(stub.getStringState("key"), is(value));
	}

	@Test
	public void testPutState() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final byte[] value = new byte[] { 0x10, 0x20, 0x30 };
		stub.putState("key",value);
		verify(handler).putState("txid", "key", ByteString.copyFrom(value));
	}

	@Test
	public void testStringState() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final String value = "TEST";
		stub.putStringState("key",value);
		verify(handler).putState("txid", "key", ByteString.copyFromUtf8(value));
	}

	@Test
	public void testDelState() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		stub.delState("key");
		verify(handler).deleteState("txid", "key");
	}

	@Test
	public void testGetStateByRange() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final String startKey = "START";
		final String endKey = "END";
		final KV[] keyValues = new KV[] {
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
		when(handler.getStateByRange("txid", startKey, endKey)).thenReturn(value);
		assertThat(stub.getStateByRange(startKey, endKey), contains(Arrays.stream(keyValues).map(KeyValueImpl::new).toArray()));
	}

	@Test
	public void testGetStateByPartialCompositeKey() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final KV[] keyValues = new KV[] {
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
		stub.getStateByPartialCompositeKey("KEY");
		verify(handler).getStateByRange("txid", "KEY", "KEY\udbff\udfff");
	}

	@Test
	public void testCreateCompositeKey() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, new ArrayList<>());
		final CompositeKey key = stub.createCompositeKey("abc", "def", "ghi", "jkl", "mno");
		assertThat(key, hasProperty("objectType", equalTo("abc")));
		assertThat(key, hasProperty("attributes", hasSize(4)));
		assertThat(key, Matchers.hasToString(equalTo("abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test
	public void testSplitCompositeKey() {
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", null, new ArrayList<>());
		final CompositeKey key = stub.splitCompositeKey("abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000");
		assertThat(key, hasProperty("objectType", equalTo("abc")));
		assertThat(key, hasProperty("attributes", contains("def", "ghi", "jkl", "mno")));
		assertThat(key, Matchers.hasToString(equalTo("abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test
	public void testGetQueryResult() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final KV[] keyValues = new KV[] {
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
		when(handler.getQueryResult("txid", "QUERY")).thenReturn(value);
		assertThat(stub.getQueryResult("QUERY"), contains(Arrays.stream(keyValues).map(KeyValueImpl::new).toArray()));
	}

	@Test
	public void testGetHistoryForKey() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final KvQueryResult.KeyModification[] keyModifications = new KvQueryResult.KeyModification[] {
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
		when(handler.getHistoryForKey("txid", "KEY")).thenReturn(value);
		assertThat(stub.getHistoryForKey("KEY"), contains(Arrays.stream(keyModifications).map(KeyModificationImpl::new).toArray()));
	}

	@Test
	public void testInvokeChaincode() {
		final Handler handler = mock(Handler.class);
		final ChaincodeStubImpl stub = new ChaincodeStubImpl("txid", handler, new ArrayList<>());
		final Chaincode.Response expectedResponse = new Chaincode.Response(Status.SUCCESS, "MESSAGE", "PAYLOAD".getBytes(UTF_8));
		when(handler.invokeChaincode("txid", "CHAINCODE_ID", new ArrayList<>())).thenReturn(expectedResponse);
		assertThat(stub.invokeChaincode("CHAINCODE_ID", new ArrayList<>()), is(expectedResponse));
	}

}
