/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.execution;

import org.hyperledger.fabric.contract.metadata.TypeSchema;
import org.hyperledger.fabric.contract.routing.TypeRegistry;
import org.hyperledger.fabric.contract.routing.impl.RegistryImpl;

public interface SerializerInterface extends RegistryImpl.Entry {

    SerializerInterface setTypeRegistry(TypeRegistry typeRegistry);

    /**
     * Convert the value supplied to a byte array, according to the TypeSchema
     *
     * @param value
     * @param ts
     * @return
     */
    byte[] toBuffer(Object value, TypeSchema ts);

    /**
     * Take the byte buffer and return the object as required
     *
     * @param buffer Byte buffer from the wire
     * @param ts     TypeSchema representing the type
     *
     * @return Object created; relies on Java auto-boxing for primitives
     *
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    Object fromBuffer(byte[] buffer, TypeSchema ts);

}