/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing;

public interface TxFunction {

	public String getName();

	public Routing getRouting();

	public Class<?> getReturnType();

	public java.lang.reflect.Parameter[] getParameters();

	public TransactionType getType();

}
