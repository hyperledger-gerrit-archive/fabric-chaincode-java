/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.routing.impl;

import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.routing.Routing;

import java.lang.reflect.Method;

public class RoutingImpl implements Routing {
    ContractInterface contract;
    Method method;
    Class clazz;
    Routing.TransactionType type;

    @Override
    public ContractInterface getContractObject() {
        return contract;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Class getContractClass() {
        return clazz;
    }

    @Override
    public Routing.TransactionType getType() {
        return type;
    }

}
