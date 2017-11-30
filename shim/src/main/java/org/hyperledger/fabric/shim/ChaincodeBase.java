/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.hyperledger.fabric.shim.Chaincode.Response.Status.INTERNAL_SERVER_ERROR;
import static org.hyperledger.fabric.shim.Chaincode.Response.Status.SUCCESS;

public abstract class ChaincodeBase implements Chaincode {

	protected static Response newSuccessResponse(String message, byte[] payload) {
		return new Response(SUCCESS, message, payload);
	}

	protected static Response newSuccessResponse() {
		return newSuccessResponse(null, null);
	}

	protected static Response newSuccessResponse(String message) {
		return newSuccessResponse(message, null);
	}

	protected static Response newSuccessResponse(byte[] payload) {
		return newSuccessResponse(null, payload);
	}

	protected static Response newErrorResponse(String message, byte[] payload) {
		return new Response(INTERNAL_SERVER_ERROR, message, payload);
	}

	protected static Response newErrorResponse() {
		return newErrorResponse(null, null);
	}

	protected static Response newErrorResponse(String message) {
		return newErrorResponse(message, null);
	}

	protected static Response newErrorResponse(byte[] payload) {
		return newErrorResponse(null, payload);
	}

	protected static Response newErrorResponse(Throwable throwable) {
		return newErrorResponse(throwable.getMessage(), printStackTrace(throwable));
	}

	private static byte[] printStackTrace(Throwable throwable) {
		if (throwable == null) return null;
		final StringWriter buffer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(buffer));
		return buffer.toString().getBytes(StandardCharsets.UTF_8);
	}
}
