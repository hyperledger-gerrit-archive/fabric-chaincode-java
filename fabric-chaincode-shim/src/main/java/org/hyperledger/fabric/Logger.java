/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Logger class to use throughout the Contract Implementation
 *
 */
public class Logger extends java.util.logging.Logger {

    public static Logger getLogger(Class<?> class1) {
        // important to add the logger to the log manager
        Logger l = new Logger(class1.getName());
        return l;
    }
	
    public static Logger getLogger(String name) {
        // important to add the logger to the log manager
        Logger l = new Logger(name);
        return l;
      }
	
    protected Logger(String name) {
        super(name, null);
        // ensure that the parent logger is set
//        this.setParent(java.util.logging.Logger.getLogger("org.hyperledger.fabric"));
        LogManager.getLogManager().addLogger(this);
    }

    @Deprecated
    public void debug(Supplier<String> msgSupplier) {
        log(Level.FINE, msgSupplier);
    }

    @Deprecated
    public void debug(String msg) {
        log(Level.FINE, msg);
    }

    @Deprecated
    public void error(String message) {
        log(Level.SEVERE, message);
    }

    @Deprecated
    public void error(Supplier<String> msgSupplier) {
        log(Level.SEVERE, msgSupplier);
    }

    public String formatError(Throwable throwable) {
        if (throwable == null)
            return null;
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

    @Deprecated
	public void warn(String msg) {
    	super.warning(msg);
//		log(Level.WARNING, msg);
	}
	
    @Deprecated
	public void warn(Supplier<String> msgSupplier) {
    	super.warning(msgSupplier);
	}


}
