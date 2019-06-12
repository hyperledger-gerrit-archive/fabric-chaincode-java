/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Class level annotation that defines the serializer that should be used to
 * convert objects to and from the wire and ledger apis
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface Serializer {
    // todo add a type here enum
}
