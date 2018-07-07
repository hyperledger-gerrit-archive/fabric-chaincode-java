/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.helper;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChannelTest {

    Channel<Integer> testChannel = new Channel<>();

    @Test
    public void channel() throws InterruptedException {
        testChannel.clear();
        testChannel.add(1);
        testChannel.add(2);
        assertEquals("Wrong item come out the channel", (long)1, (long)testChannel.take());
        testChannel.close();
        try {
            testChannel.take();
        } catch (InterruptedException e) {
            return;
        }
        fail("closed channel didn't throw exception");
    }

}