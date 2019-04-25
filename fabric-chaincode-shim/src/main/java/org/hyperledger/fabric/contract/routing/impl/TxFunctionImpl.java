/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing.impl;

import java.lang.reflect.Method;

import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.annotation.Init;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.routing.ContractDefinition;
import org.hyperledger.fabric.contract.routing.Routing;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.hyperledger.fabric.contract.routing.TxFunction;

public class TxFunctionImpl implements TxFunction {
	private static Logger logger = Logger.getLogger(TxFunctionImpl.class);

	private Method method;
	private ContractDefinition contract;
	private TransactionType type;

	/**
	 * New TxFunction Definition Impl
	 *
	 * @param m   Reflect method object
	 * @param contract   ContractDefinition this is part of
	 */
	public TxFunctionImpl(Method m, ContractDefinition contract) {

        this.method = m;
        this.contract = contract;

        if (m.getAnnotation(Transaction.class) != null) {
            logger.debug("Found Transaction method: " + m.getName());
            if (m.getAnnotation(Transaction.class).submit()) {
                this.type = TransactionType.INVOKE;
            } else {
                this.type = TransactionType.QUERY;
            }

        }
        if (m.getAnnotation(Init.class) != null) {
            this.type = TransactionType.INIT;
            logger.debug(()-> "Found Init method: " + m.getName());
        }
	}

	@Override
	public String getName() {
		return this.method.getName();
	}

	@Override
	public Routing getRouting() {
	    RoutingImpl rd = new RoutingImpl();
	    rd.contract = this.contract.getContractImpl();
	    rd.method = this.method;
	    rd.type = this.type;
	    rd.clazz = rd.contract.getClass();

		return rd;
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



}
