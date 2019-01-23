/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.routing;

import java.lang.reflect.Method;

public class RoutingData {
    Object chaincode;
    Method method;
    Class clazz;
    TransactionType type;

    public Object getChaincodeOject() {
        return chaincode;
    }

    public Method getMethod() {
        return method;
    }

    public Class getChaincodeClass() {
        return clazz;
    }

    public TransactionType getType() {
        return type;
    }

    public enum TransactionType {
        INIT,
        UPGRADE,
        INVOKE,
        QUERY
    }
}
