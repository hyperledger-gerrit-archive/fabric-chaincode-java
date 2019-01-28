/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact;

public interface ContractInterface {
    default Context getContext() { throw new IllegalStateException("getContext default implementation can't be directly invoked"); }
    default void unknownTransaction() {
        throw new IllegalStateException("Undefined contract method called");
    }
    default void beforeTransaction() {}
    default void afterTransaction() {}
}
