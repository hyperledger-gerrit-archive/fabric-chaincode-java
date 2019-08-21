/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim.helper;

import java.io.Closeable;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import org.hyperledger.fabric.metrics.Metrics;

@SuppressWarnings("serial")
public class Channel<E> extends LinkedBlockingQueue<E> implements Closeable {

    private boolean closed = false;
    private String purpose = "unknown";
    private HashSet<Thread> waiting = new HashSet<>();

    public Channel() {
    	
    }
    
    public Channel(String purpose) {
    	this.purpose = purpose;
    }
    
    // TODO add other methods to secure closing behavior

    @Override
    public E take() throws InterruptedException {
        synchronized (waiting) {
            if (closed) throw new InterruptedException("Channel closed");
            waiting.add(Thread.currentThread());
        }
        //Metrics.getProvider().setChannelSize(purpose,super.size());
        E e = super.take();
        synchronized (waiting) {
            waiting.remove(Thread.currentThread());
        }
        return e;
    }

    @Override
    public boolean add(E e) {
        if (closed) {
            throw new IllegalStateException("Channel is closed");
        }
        return super.add(e);
    }

    @Override
    public void close() {
        synchronized (waiting) {
            closed = true;
            for (Thread t : waiting) {
                t.interrupt();
            }
            waiting.clear();
            clear();
        }
    }

}
