/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.execution;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.contract.metadata.TypeSchema;
import org.hyperledger.fabric.contract.routing.TypeRegistry;
import org.hyperledger.fabric.contract.routing.impl.TypeRegistryImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JSONTransactionSerializerTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void toBuffer() {
		TypeRegistry tr = new TypeRegistryImpl();
		JSONTransactionSerializer serializer = new JSONTransactionSerializer(tr);

		byte[] bytes = serializer.toBuffer("hello world", TypeSchema.typeConvert(String.class));
		assertThat(new String(bytes, StandardCharsets.UTF_8), equalTo("hello world"));

		bytes = serializer.toBuffer(42, TypeSchema.typeConvert(Integer.class));
		assertThat(new String(bytes, StandardCharsets.UTF_8), equalTo("42"));

		bytes = serializer.toBuffer(true, TypeSchema.typeConvert(Boolean.class));
		assertThat(new String(bytes, StandardCharsets.UTF_8), equalTo("true"));

		bytes = serializer.toBuffer(new MyType(), TypeSchema.typeConvert(MyType.class));
		assertThat(new String(bytes, StandardCharsets.UTF_8), equalTo("{}"));

		bytes = serializer.toBuffer(new MyType().setValue("Hello"), TypeSchema.typeConvert(MyType.class));
		assertThat(new String(bytes, StandardCharsets.UTF_8), equalTo("{\"value\":\"Hello\"}"));

		MyType array[] = new MyType[2];
		array[0] = new MyType().setValue("hello");
		array[1] = new MyType().setValue("world");
		bytes = serializer.toBuffer(array, TypeSchema.typeConvert(MyType[].class));
		
		byte[] buffer = "[{\"value\":\"hello\"},{\"value\":\"world\"}]".getBytes(StandardCharsets.UTF_8);
		
		System.out.println(new String(buffer,StandardCharsets.UTF_8));
		System.out.println(new String(bytes,StandardCharsets.UTF_8));
		assertThat(bytes, equalTo(buffer));
	}

	@Test
	public void fromBuffer() throws InstantiationException, IllegalAccessException {
		byte[] buffer = "[{\"value\":\"hello\"},{\"value\":\"world\"}]".getBytes(StandardCharsets.UTF_8);

		TypeRegistry tr = new TypeRegistryImpl();
		tr.addDataType(MyType.class);

		JSONTransactionSerializer serializer = new JSONTransactionSerializer(tr);
		TypeSchema ts = TypeSchema.typeConvert(MyType[].class);
		MyType[] o = (MyType[]) serializer.fromBuffer(buffer, ts);
		assertThat(o[0].toString(),equalTo("++++ MyType: hello"));
		assertThat(o[1].toString(),equalTo("++++ MyType: world"));
		
		

	}
}
