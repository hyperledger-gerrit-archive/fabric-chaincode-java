/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.fabric.shim;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ChaincodeBaseTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

	@Test
	public void newSuccessResponseNoArgs() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse();
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newSuccessResponseWithMessageOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse("message");
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newSuccessResponseWithPayloadOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse("payload".getBytes());
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", equalTo("payload".getBytes())));
	}

	@Test
	public void newSuccessResponse() throws Exception {
		Chaincode.Response response = ChaincodeBase.newSuccessResponse("message", "payload".getBytes());
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.SUCCESS)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", equalTo("payload".getBytes())));
	}

	@Test
	public void newErrorResponseWithException() throws Exception {
		RuntimeException exception = new RuntimeException("message");
		StringWriter stack = new StringWriter();
		Chaincode.Response response = ChaincodeBase.newErrorResponse(exception);
		exception.printStackTrace(new PrintWriter(stack));
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", equalTo(stack.toString().getBytes())));
	}

	@Test
	public void newErrorResponseNoArgs() throws Exception {
		Chaincode.Response response = ChaincodeBase.newErrorResponse();
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newErrorResponseWithMessageOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newErrorResponse("message");
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", equalTo("message")));
		assertThat(response, hasProperty("payload", nullValue()));
	}

	@Test
	public void newErrorResponseWithPayloadOnly() throws Exception {
		Chaincode.Response response = ChaincodeBase.newErrorResponse("payload".getBytes());
		assertNotNull(response);
		assertThat(response, hasProperty("status", equalTo(Chaincode.Response.Status.INTERNAL_SERVER_ERROR)));
		assertThat(response, hasProperty("message", nullValue()));
		assertThat(response, hasProperty("payload", equalTo("payload".getBytes())));
	}
}