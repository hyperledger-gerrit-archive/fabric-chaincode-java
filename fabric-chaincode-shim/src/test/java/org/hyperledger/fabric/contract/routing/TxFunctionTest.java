/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.routing.impl.TxFunctionImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TxFunctionTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    class TestObject implements ContractInterface {

        @Transaction()
        public void testMethod1(Context ctx) {

        }

        @Transaction()
        public void testMethod2(Context ctx, @Property(schema = { "a", "b" }) int arg) {

        }
    }

    @Before
    public void beforeEach() {
    }

    @Test
    public void constructor() throws NoSuchMethodException, SecurityException {
        TestObject test = new TestObject();
        ContractDefinition cd = mock(ContractDefinition.class);

        TxFunction txfn = new TxFunctionImpl(test.getClass().getMethod("testMethod1", new Class[] { Context.class }),
                cd);
        String name = txfn.getName();
        assertEquals(name, "testMethod1");

        assertThat(txfn.toString(), startsWith("testMethod1"));
    }

    @Test
    public void property() throws NoSuchMethodException, SecurityException {
        TestObject test = new TestObject();
        ContractDefinition cd = mock(ContractDefinition.class);

        TxFunction txfn = new TxFunctionImpl(
                test.getClass().getMethod("testMethod2", new Class[] { Context.class, int.class }), cd);
        String name = txfn.getName();
        assertEquals(name, "testMethod2");

        assertThat(txfn.toString(), startsWith("testMethod2"));
    }
}