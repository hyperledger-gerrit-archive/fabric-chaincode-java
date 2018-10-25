/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.shim.ledger;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

public interface QueryResultsIteratorWithMetadata<T> extends Iterable<T>, AutoCloseable {
    ChaincodeShim.QueryResponseMetadata getMetadata();
}
