/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.metadata.TypeSchema;
import org.hyperledger.fabric.contract.routing.ContractDefinition;
import org.hyperledger.fabric.contract.routing.ParameterDefinition;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.hyperledger.fabric.contract.routing.TxFunction;

public class TxFunctionImpl implements TxFunction {
	private static Logger logger = Logger.getLogger(TxFunctionImpl.class);

	private Method method;
	private TransactionType type;
	private Routing routing;
	private TypeSchema returnSchema;
	private ArrayList<ParameterDefinition> paramsList = new ArrayList<>();

	public class RoutingImpl implements Routing {
		ContractInterface contract;
		Method method;
		Class<? extends ContractInterface> clazz;

		public RoutingImpl(Method method, ContractInterface contract) {
			this.method = method;
			this.contract = contract;
			clazz = contract.getClass();
		}

		@Override
		public ContractInterface getContractObject() {
			return contract;
		}

		@Override
		public Method getMethod() {
			return method;
		}

		@Override
		public Class<? extends ContractInterface> getContractClass() {
			return clazz;
		}

		@Override
		public String toString() {
			return method.getName() + ":" + clazz.getCanonicalName() + ":" + contract.getClass().getCanonicalName();
		}
	}

	/**
	 * New TxFunction Definition Impl
	 *
	 * @param m        Reflect method object
	 * @param contract ContractDefinition this is part of
	 */
	public TxFunctionImpl(Method m, ContractDefinition contract) {

		this.method = m;
		if (m.getAnnotation(Transaction.class) != null) {
			logger.debug("Found Transaction method: " + m.getName());
			if (m.getAnnotation(Transaction.class).submit()) {
				this.type = TransactionType.INVOKE;
			} else {
				this.type = TransactionType.QUERY;
			}

		}

		this.routing = new RoutingImpl(m, contract.getContractImpl());

		// set the return schema
		this.returnSchema =  TypeSchema.typeConvert(m.getReturnType());

		// parameter processing
		java.lang.reflect.Parameter[] params = method.getParameters();
		for (java.lang.reflect.Parameter parameter : params) {
			TypeSchema paramMap = new TypeSchema();
			paramMap.put("name", parameter.getName());
			paramMap.put("schema", TypeSchema.typeConvert(parameter.getType()));
			ParameterDefinition pd = new ParameterDefinitionImpl(parameter.getName(),parameter.getClass(),paramMap,parameter);
			paramsList.add(pd);
		}
	}

	@Override
	public String getName() {
		return this.method.getName();
	}

	@Override
	public Routing getRouting() {
		return this.routing;
	}

	@Override
	public Class<?> getReturnType() {
		return method.getReturnType();
	}

	@Override
	public java.lang.reflect.Parameter[] getParameters() {
		return method.getParameters();
	}

	@Override
	public TransactionType getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return this.method.getName() + " @" + Integer.toHexString(System.identityHashCode(this));
	}

	@Override
	public void setReturnSchema(TypeSchema returnSchema) {
		this.returnSchema = returnSchema;
	}

	@Override
	public void setParameterSchema(ArrayList<TypeSchema> paramsList) {
//		this.paramsList = paramsList;
	}

	public ArrayList<ParameterDefinition> getParamsList() {
		return paramsList;
	}

	public void setParamsList(ArrayList<ParameterDefinition> paramsList) {
		this.paramsList = paramsList;
	}

	@Override
	public TypeSchema getReturnSchema() {
		return returnSchema;
	}

	@Override
	public void setParameterDefinitions(ArrayList<ParameterDefinition> list) {
		this.paramsList = list;

	}

}
