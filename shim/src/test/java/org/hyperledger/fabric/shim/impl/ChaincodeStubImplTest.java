package org.hyperledger.fabric.shim.impl;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.hyperledger.fabric.protos.ledger.queryresult.KvQueryResult;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ChaincodeStubImplTest {
	@Test
	public void testArgs() throws Exception {
		List<ByteString> args = Arrays.asList(ByteString.copyFromUtf8("func"), ByteString.copyFromUtf8("arg0"));
		ChaincodeStubImpl stub = new ChaincodeStubImpl("123", null, args);
		assertEquals("123", stub.getTxId());
		assertEquals(2, stub.getArgs().size());
		assertEquals("func", new String(stub.getArgs().get(0)));
		assertEquals("arg0", new String(stub.getArgs().get(1)));
		assertEquals(2, stub.getStringArgs().size());
		assertEquals("func", stub.getStringArgs().get(0));
		assertEquals("arg0", stub.getStringArgs().get(1));
		assertEquals(1, stub.getParameters().size());
		assertEquals("arg0", stub.getParameters().get(0));
		assertEquals("func", stub.getFunction());
	}

	@Test
	public void testEvent() throws Exception {
		ChaincodeStubImpl stub = new ChaincodeStubImpl("123", null, Collections.emptyList());
		final String eventName = "eventName", eventPayload = "eventPayload";
		stub.setEvent(eventName, eventPayload.getBytes());
		ChaincodeEventPackage.ChaincodeEvent event = stub.getEvent();
		assertNotNull(event);
		assertEquals(event.getEventName(), eventName);
		assertEquals(event.getPayload().toStringUtf8(), eventPayload);

		stub.setEvent(eventName, null);
		event = stub.getEvent();
		assertNotNull(event);
		assertEquals(event.getEventName(), eventName);
		assertEquals(event.getPayload().toStringUtf8(), "");
	}

	@Test
	public void testState() throws Exception {
		Handler mock = mock(Handler.class);
		final String txId = "123", key = "abc", value = "hello";
		ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, mock, Collections.emptyList());

		when(mock.getState(txId, key)).thenReturn(ByteString.copyFromUtf8(value));
		assertArrayEquals(value.getBytes(), stub.getState(key));
		verify(mock).getState(txId, key);

		stub.putState(key, value.getBytes());
		verify(mock).putState(txId, key, ByteString.copyFromUtf8(value));

		stub.delState(key);
		verify(mock).deleteState(txId, key);
	}

	@Test
	public void testQuery() throws Exception {
		Handler mock = mock(Handler.class);
		final String txId = "123", key = "abc", value = "hello", query = "SELECT * FROM table";
		ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, mock, Collections.emptyList());

		final String unspecifiedKey = new String(Character.toChars(0x000001));
		when(mock.getStateByRange(txId, unspecifiedKey, unspecifiedKey + "\udbff\udfff"))
				.thenReturn(queryResponse(txId, key, value));
		checkQueryResult(key, value, stub.getStateByPartialCompositeKey(null));
		verify(mock).getStateByRange(txId, unspecifiedKey, unspecifiedKey + "\udbff\udfff");

		when(mock.getQueryResult(txId, query)).thenReturn(queryResponse(txId, key, value));
		checkQueryResult(key, value, stub.getQueryResult(query));
		verify(mock).getQueryResult(txId, query);
	}

	@Test(expected = InvalidProtocolBufferException.class)
	public void testQueryWithException() throws Throwable{
		Handler mock = mock(Handler.class);
		final String txId = "123", query = "SELECT * FROM table";
		ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, mock, Collections.emptyList());
		when(mock.getQueryResult(txId, query))
				.thenReturn(ChaincodeShim.QueryResponse.newBuilder()
						.setId(txId)
						.setHasMore(false)
						.addResults(ChaincodeShim.QueryResultBytes.newBuilder()
								.setResultBytes(ByteString.copyFromUtf8("exception"))
								.build())
						.build());
		try {
			stub.getQueryResult(query).iterator().next();
		} catch (RuntimeException e) {
			throw e.getCause();
		}
	}

	private ChaincodeShim.QueryResponse queryResponse(String txId, String key, String value) {
		return ChaincodeShim.QueryResponse.newBuilder()
				.setId(txId)
				.setHasMore(false)
				.addResults(ChaincodeShim.QueryResultBytes.newBuilder()
						.setResultBytes(KvQueryResult.KV.newBuilder()
								.setKey(key)
								.setValue(ByteString.copyFromUtf8(value))
								.build()
								.toByteString())
						.build())
				.build();
	}

	private void checkQueryResult(String key, String value, QueryResultsIterator<KeyValue> queryResult) {
		Iterator<KeyValue> iterator = queryResult.iterator();
		assertTrue(iterator.hasNext());
		KeyValue next = iterator.next();
		assertEquals(next.getKey(), key);
		assertArrayEquals(next.getValue(), value.getBytes());
		assertEquals(next.getStringValue(), value);
		assertFalse(iterator.hasNext());
	}


	@Test
	public void testGetHistoryForKey() throws Exception {
		Handler mock = mock(Handler.class);
		final String txId = "123", key = "abc", value = "hello";
		ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, mock, Collections.emptyList());

		final int time = 12345;
		final boolean isDeleted = true;
		when(mock.getHistoryForKey(txId, key)).thenReturn(
				getHistoryForKeyResponse(time, isDeleted, txId, value));
		checkGetHistoryForKeyResponse(time, isDeleted, txId, value, stub.getHistoryForKey(key));
		verify(mock).getHistoryForKey(txId, key);
	}

	@Test(expected = InvalidProtocolBufferException.class)
	public void testGetHistoryForKeyWithException() throws Throwable{
		Handler mock = mock(Handler.class);
		final String txId = "123", key = "key";
		ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, mock, Collections.emptyList());
		when(mock.getHistoryForKey(txId, key))
				.thenReturn(ChaincodeShim.QueryResponse.newBuilder()
						.setId(txId)
						.setHasMore(false)
						.addResults(ChaincodeShim.QueryResultBytes.newBuilder()
								.setResultBytes(ByteString.copyFromUtf8("exception"))
								.build())
						.build());
		try {
			stub.getHistoryForKey(key).iterator().next();
		} catch (RuntimeException e) {
			throw e.getCause();
		}
	}

	private ChaincodeShim.QueryResponse getHistoryForKeyResponse(int time, boolean isDeleted, String txId, String value) {
		return ChaincodeShim.QueryResponse.newBuilder()
				.setId(txId)
				.setHasMore(false)
				.addResults(ChaincodeShim.QueryResultBytes.newBuilder()
						.setResultBytes(KvQueryResult.KeyModification.newBuilder()
								.setTimestamp(Timestamp.newBuilder().setNanos(time).build())
								.setIsDelete(isDeleted)
								.setTxId(txId)
								.setValue(ByteString.copyFromUtf8(value))
								.build()
								.toByteString())
						.build())
				.build();
	}

	private void checkGetHistoryForKeyResponse(int time, boolean isDeleted, String txId, String value, QueryResultsIterator<KeyModification> queryResult) {
		Iterator<KeyModification> iterator = queryResult.iterator();
		assertTrue(iterator.hasNext());
		KeyModification next = iterator.next();
		assertEquals(time, next.getTimestamp().getNano());
		assertEquals(isDeleted, next.isDeleted());
		assertEquals(txId, next.getTxId());
		assertArrayEquals(value.getBytes(), next.getValue());
		assertEquals(value, next.getStringValue());
	}

	@Test
	public void testCompositeKey() throws Exception {
		ChaincodeStubImpl stub = new ChaincodeStubImpl("123", null, Collections.emptyList());
		final String objType = "abc", attr = "def", compositeKey = "\u0000" + objType + "\u0000" + attr + "\u0000";
		assertEquals(compositeKey, stub.createCompositeKey(objType, attr).toString());
		assertEquals(compositeKey, stub.splitCompositeKey(compositeKey).toString());
	}

	@Test
	public void testInvokeChaincode() throws Exception {
		Handler mock = mock(Handler.class);
		final String txId = "123", ccName = "chaincode", arg0 = "arg0", arg1 = "arg1",
				msg = "message", payload = "abc", channel = "def";
		final Chaincode.Response.Status status = Chaincode.Response.Status.SUCCESS;
		ChaincodeStubImpl stub = new ChaincodeStubImpl(txId, mock, Collections.emptyList());
		when(mock.invokeChaincode(eq(txId), eq(ccName), anyList()))
				.thenReturn(new Chaincode.Response(status, msg, payload.getBytes()));
		checkChaincodeResponse(mock, txId, ccName, arg0, arg1, status, msg, payload, stub.invokeChaincodeWithStringArgs(ccName, arg0, arg1));

		reset(mock);
		when(mock.invokeChaincode(eq(txId), eq(ccName + "/" + channel), anyList()))
				.thenReturn(new Chaincode.Response(status, msg, payload.getBytes()));
		checkChaincodeResponse(mock, txId, ccName + "/" + channel, arg0, arg1, status, msg, payload, stub.invokeChaincodeWithStringArgs(ccName, Arrays.asList(arg0, arg1), channel));
	}

	private void checkChaincodeResponse(Handler mock, String txId, String ccName, String arg0, String arg1,
	                                    Chaincode.Response.Status status, String message, String payload,
	                                    Chaincode.Response response) {
		assertNotNull(response);
		assertEquals(status, response.getStatus());
		assertEquals(message, response.getMessage());
		assertArrayEquals(payload.getBytes(), response.getPayload());
		assertEquals(payload, response.getStringPayload());

		ArgumentCaptor<List<byte[]>> captor = ArgumentCaptor.forClass(List.class);
		verify(mock).invokeChaincode(eq(txId), eq(ccName), captor.capture());
		Iterator<byte[]> iterator = captor.getValue().iterator();
		assertTrue(iterator.hasNext());
		assertArrayEquals(arg0.getBytes(), iterator.next());
		assertTrue(iterator.hasNext());
		assertArrayEquals(arg1.getBytes(), iterator.next());
		assertFalse(iterator.hasNext());
	}
}