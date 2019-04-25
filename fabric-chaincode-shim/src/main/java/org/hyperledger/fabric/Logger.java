/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.hyperledger.fabric.contract.routing.impl.ContractScannerImpl;

public class Logger extends java.util.logging.Logger{

	protected Logger(String name) {
		super(name, null);
	}

	public static Logger getLogger(String name) {
		return new Logger(name);
	}
	
    public void debug(Supplier<String> msgSupplier) {
        log(Level.FINEST, msgSupplier);
    }
    
    public void debug(String msg) {
        log(Level.FINEST, msg);
    }

	public static Logger getLogger(Class<?> class1) {
		return Logger.getLogger(class1.getName());
	}

	public void error(String message) {
		log(Level.SEVERE,message);
	}
	
    public void error(Supplier<String> msgSupplier) {
        log(Level.SEVERE, msgSupplier);
    }
    
    public String formatError(Throwable throwable) {
        if (throwable == null) return null;
        final StringWriter buffer = new StringWriter();
        buffer.append(throwable.getMessage());
        throwable.printStackTrace(new PrintWriter(buffer));
        
        Throwable cause = throwable.getCause();
        if (cause != null) {
        	buffer.append(".. caused by ..");
        	buffer.append(this.formatError(cause));	
        }
        
        return buffer.toString();
    }  

}
