/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.execution.impl;

import org.hyperledger.fabric.contact.Context;
import org.hyperledger.fabric.contact.execution.InvocationRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ContractInvocationRequest implements InvocationRequest {
    String namespace;
    String method;
    List<byte[]> args = Collections.emptyList();

    public ContractInvocationRequest(Context context) {
        String func = context.getFunction();
        String funcParts[] = func.split(":");

        if (funcParts.length == 2) {
            namespace = funcParts[0];
            method = funcParts[1];
        } else {
            namespace = DEFAULT_NAMESPACE;
            method = funcParts[1];
        }

        args = context.getArgs().stream().skip(1).collect(Collectors.toList());
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public List<byte[]> getArgs() {
        return args;
    }

    @Override
    public String getRequestName() {
        return namespace + ":" + method;
    }

}
