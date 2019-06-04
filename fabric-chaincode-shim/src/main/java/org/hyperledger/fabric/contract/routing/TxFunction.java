/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.metadata.TypeSchema;

public interface TxFunction {

	interface Routing {
	    ContractInterface getContractObject();

	    Method getMethod();

	    Class<?> getContractClass();

	}

	String getName();

    Routing getRouting();

	Class<?> getReturnType();

	java.lang.reflect.Parameter[] getParameters();

	TransactionType getType();

	void setReturnSchema(TypeSchema returnSchema);

	TypeSchema getReturnSchema();

	void setParameterSchema(ArrayList<TypeSchema> paramsList);

	void setParameterDefinitions(ArrayList<ParameterDefinition> list);

	ArrayList<ParameterDefinition> getParamsList();
}
