/*
Copyright IBM 2017 All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.hyperledger.fabric.shim.ledger;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CompositeKeyTest {
	@Test
	public void testValidateSimpleKeys() {
		CompositeKey.validateSimpleKeys("abc", "def", "ghi");
	}

	@Test(expected = CompositeKeyFormatException.class)
	public void testValidateSimpleKeysException() throws Exception {
		CompositeKey.validateSimpleKeys("\u0000abc");
	}

	@Test
	public void testCompositeKeyStringStringArray() {
		final CompositeKey key = new CompositeKey("abc", "def", "ghi", "jkl", "mno");
		assertThat(key.getObjectType(), is(equalTo("abc")));
		assertThat(key.getAttributes(), hasSize(4));
		assertThat(key.toString(), is(equalTo("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test
	public void testCompositeKeyStringListOfString() {
		final CompositeKey key = new CompositeKey("abc", Arrays.asList("def", "ghi", "jkl", "mno"));
		assertThat(key.getObjectType(), is(equalTo("abc")));
		assertThat(key.getAttributes(), hasSize(4));
		assertThat(key.toString(), is(equalTo("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test
	public void testEmptyAttributes() {
		final CompositeKey key = new CompositeKey("abc");
		assertThat(key.getObjectType(), is(equalTo("abc")));
		assertThat(key.getAttributes(), hasSize(0));
		assertThat(key.toString(), is(equalTo("\u0000abc\u0000")));
	}

	@Test(expected=CompositeKeyFormatException.class)
	public void testCompositeKeyWithInvalidObjectTypeDelimiter() {
		new CompositeKey("ab\u0000c", Arrays.asList("def", "ghi", "jkl", "mno"));
	}

	@Test(expected=CompositeKeyFormatException.class)
	public void testCompositeKeyWithInvalidAttributeDelimiter() {
		new CompositeKey("abc", Arrays.asList("def", "ghi", "j\u0000kl", "mno"));
	}

	@Test(expected=CompositeKeyFormatException.class)
	public void testCompositeKeyWithInvalidObjectTypeMaxCodePoint() {
		new CompositeKey("ab\udbff\udfffc", Arrays.asList("def", "ghi", "jkl", "mno"));
	}
	@Test(expected=CompositeKeyFormatException.class)
	public void testCompositeKeyWithInvalidAttributeMaxCodePoint() {
		new CompositeKey("abc", Arrays.asList("def", "ghi", "jk\udbff\udfffl", "mno"));
	}

	@Test
	public void testGetObjectType() {
		final CompositeKey key = new CompositeKey("abc", Arrays.asList("def", "ghi", "jkl", "mno"));
		assertThat(key.getObjectType(), is(equalTo("abc")));
	}

	@Test
	public void testGetAttributes() {
		final CompositeKey key = new CompositeKey("abc", Arrays.asList("def", "ghi", "jkl", "mno"));
		assertThat(key.getObjectType(), is(equalTo("abc")));
		assertThat(key.getAttributes(), hasSize(4));
		assertThat(key.getAttributes(), contains("def", "ghi", "jkl", "mno"));
	}

	@Test
	public void testToString() {
		final CompositeKey key = new CompositeKey("abc", Arrays.asList("def", "ghi", "jkl", "mno"));
		assertThat(key.toString(), is(equalTo("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test
	public void testParseCompositeKey() {
		final CompositeKey key = CompositeKey.parseCompositeKey("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000");
		assertThat(key.getObjectType(), is(equalTo("abc")));
		assertThat(key.getAttributes(), hasSize(4));
		assertThat(key.getAttributes(), contains("def", "ghi", "jkl", "mno"));
		assertThat(key.toString(), is(equalTo("\u0000abc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000")));
	}

	@Test(expected=CompositeKeyFormatException.class)
	public void testParseCompositeKeyInvalidObjectType() {
		CompositeKey.parseCompositeKey("ab\udbff\udfffc\u0000def\u0000ghi\u0000jkl\u0000mno\u0000");
	}

	@Test(expected=CompositeKeyFormatException.class)
	public void testParseCompositeKeyInvalidAttribute() {
		CompositeKey.parseCompositeKey("abc\u0000def\u0000ghi\u0000jk\udbff\udfffl\u0000mno\u0000");
	}

}
