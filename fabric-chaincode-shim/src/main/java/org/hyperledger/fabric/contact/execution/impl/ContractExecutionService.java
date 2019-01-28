/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.execution.impl;

import org.hyperledger.fabric.contact.ContextFactory;
import org.hyperledger.fabric.contact.ContractInterface;
import org.hyperledger.fabric.contact.execution.ExecutionService;
import org.hyperledger.fabric.contact.execution.InvocationRequest;
import org.hyperledger.fabric.contact.ContractFromChaincode;
import org.hyperledger.fabric.contact.routing.Routing;
import org.hyperledger.fabric.shim.Chaincode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContractExecutionService implements ExecutionService {

    Map<String, Object> proxies = new HashMap<>();

    @Override
    public Chaincode.Response executeRequest(Routing rd, InvocationRequest req) {
        final ContractInterface contractObject = rd.getContractObject();
        final Class<?> contractClass = rd.getContractClass();
        if (!proxies.containsKey(req.getNamespace())) {
            proxies.put(req.getNamespace(), createProxyForContract(contractClass, contractObject));
        }

        Object proxyObject = proxies.get(req.getNamespace());
        final List<Object> args = convertArgs(req.getArgs(), rd.getMethod().getParameterTypes());

        Chaincode.Response response = null;
        try {
            response = (Chaincode.Response)rd.getMethod().invoke(proxyObject, args.toArray());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return response;
    }

    private List<Object> convertArgs(List<byte[]> stubArgs, Class<?>[] methodParameterTypes) {
        List<Object> args = new ArrayList<>();
        for (int i = 1; i < methodParameterTypes.length; i++) {
            Class<?> param = methodParameterTypes[i];
            if (param.isArray()) {
                args.add(stubArgs.get(i));
            } else {
                args.add(new String(stubArgs.get(i), StandardCharsets.UTF_8));
            }
        }
        return args;
    }

    private Object createProxyForContract(final Class<?> contractClass, final ContractInterface contractObject) {
        return Proxy.newProxyInstance(ContractFromChaincode.class.getClassLoader(), new Class[] { contractClass },
                (proxy, method, methodArgs) -> {
                    if (method.getName().equals("getContext")) {
                        return ContextFactory.getInstance().getContext();
                    } else {
                        contractObject.beforeTransaction();;
                        Object result = contractClass.getDeclaredMethod(method.getName(), method.getParameterTypes()).invoke(contractObject, methodArgs);
                        contractObject.afterTransaction();
                        return result;
                    }
                });
    }
}
