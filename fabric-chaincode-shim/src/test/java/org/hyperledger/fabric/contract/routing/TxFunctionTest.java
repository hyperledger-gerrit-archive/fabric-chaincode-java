/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.routing.impl.TxFunctionImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class TxFunctionTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    class TestObject {

    	@Transaction()
    	public void testMethod1() {

    	}

    	@Transaction()
    	public void testMethod2() {

    	}
    }

    @Before
    public void beforeEach() {
    }

    @Test
    public void constructor() throws NoSuchMethodException, SecurityException {
    	TestObject test = new TestObject();
    	ContractDefinition cd = mock(ContractDefinition.class);
    	TxFunction txfn = new TxFunctionImpl(test.getClass().getMethod("testMethod1", null), cd );
    	String name = txfn.getName();
    	assertEquals(name, "testMethod1");

    	assertThat(txfn.toString(),startsWith("testMethod1"));
    }
}