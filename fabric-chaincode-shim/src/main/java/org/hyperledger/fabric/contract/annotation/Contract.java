/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.annotation;

import io.swagger.v3.oas.annotations.info.Info;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Class level annotation that identifies this class as being a contract.
 * Can supply information and an alternative name for the contract rather than the classname
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface Contract {
    Info info();
    String namespace() default "";
}
