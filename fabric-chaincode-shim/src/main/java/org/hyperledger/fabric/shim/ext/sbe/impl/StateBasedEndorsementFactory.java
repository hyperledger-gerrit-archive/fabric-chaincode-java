/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim.ext.sbe.impl;

import org.hyperledger.fabric.shim.ext.sbe.StateBasedEndorsement;

public class StateBasedEndorsementFactory {
    static StateBasedEndorsementFactory instance;
    public static synchronized StateBasedEndorsementFactory getInstance() {
        if (instance == null) {
            instance = new StateBasedEndorsementFactory();
        }
        return instance;
    }

    public StateBasedEndorsement newStateBasedEndorsement(byte[] ep) {
        return new StateBasedEndorsementImpl(ep);
    }
}
