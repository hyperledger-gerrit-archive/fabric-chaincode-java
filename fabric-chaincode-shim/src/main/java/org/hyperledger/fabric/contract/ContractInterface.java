/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Interface all contracts should implement
 */
public interface ContractInterface {

    /**
     * Create context from {@link ChaincodeStub}, default impl provided, but can be
     * overwritten by contract
     *
     * @param stub
     * @return
     */
    default Context createContext(ChaincodeStub stub) {
        return ContextFactory.getInstance().createContext(stub);
    }

    /**
     * Invoked once method for transaction not exist in contract
     */
    default void unknownTransaction(Context ctx) {
        throw new IllegalStateException("Undefined contract method called");
    }

    /**
     * Invoke before each transaction method
     */
    default void beforeTransaction(Context ctx) {
    }

    /**
     * Invoke after each transaction method
     */
    default void afterTransaction(Context ctx, Object result) {
    }
}
