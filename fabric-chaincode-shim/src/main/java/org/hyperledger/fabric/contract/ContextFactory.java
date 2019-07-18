/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import java.io.IOException;
import java.security.cert.CertificateException;

import org.hyperledger.fabric.shim.ChaincodeStub;
import org.json.JSONException;

/**
 * Factory to create {@link Context} from {@link ChaincodeStub} by wrapping stub
 * with dynamic proxy.
 */
public class ContextFactory {
    private static ContextFactory cf;

    static synchronized public ContextFactory getInstance() {
        if (cf == null) {
            cf = new ContextFactory();
        }
        return cf;
    }

    public Context createContext(final ChaincodeStub stub) {
        Context newContext = new Context(stub);
        try {
            newContext.setClientIdentity(new ClientIdentity(stub));
        } catch (CertificateException | JSONException | IOException e) {
            throw new ContractRuntimeException("Could not create new client identity", e);
        }

        return newContext;
    }

}
