/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.routing.impl;

import org.hyperledger.fabric.contact.ContractInterface;
import org.hyperledger.fabric.contact.annotation.Contract;
import org.hyperledger.fabric.contact.annotation.Init;
import org.hyperledger.fabric.contact.annotation.Transaction;
import org.hyperledger.fabric.contact.annotation.Upgrade;
import org.hyperledger.fabric.contact.execution.InvocationRequest;
import org.hyperledger.fabric.contact.routing.ContractScanner;
import org.hyperledger.fabric.contact.routing.Routing;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ContractScannerImpl implements ContractScanner {
    Map<String, Routing> routingData = new HashMap<>();
    Map<String, ContractInterface> contracts = new HashMap<>();

    @Override
    public void findAndSetContracts() throws IllegalAccessException, InstantiationException {
        ArrayList<URL> urls = new ArrayList<>();
        ClassLoader[] classloaders = {
                getClass().getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        };
        for (int i = 0; i < classloaders.length; i++) {
            if (classloaders[i] instanceof URLClassLoader) {
                urls.addAll(Arrays.asList(((URLClassLoader) classloaders[i]).getURLs()));
            } else {
                throw new RuntimeException("classLoader is not an instanceof URLClassLoader");
            }
        }

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addUrls(urls);
        configurationBuilder.addUrls(ClasspathHelper.forJavaClassPath());
        Reflections ref = new Reflections();

        System.out.println("Searching chaincode class in urls: " + urls);

        for (Class<?> cl : ref.getTypesAnnotatedWith(Contract.class)) {
            String namespace = cl.getAnnotation(Contract.class).namespace();
            if (namespace != null && !namespace.isEmpty()) {
                namespace = InvocationRequest.DEFAULT_NAMESPACE;
            }
            System.out.println("Found class: " + cl.getCanonicalName());
            System.out.println("Searching init and invoke annotated methods");
            if (ContractInterface.class.isAssignableFrom(cl)) {
                if (contracts.containsKey(cl.getName()))
                    for (Method m : cl.getMethods()) {
                        if ((m.getAnnotation(Transaction.class) != null) || (m.getAnnotation(Init.class) != null) || (m.getAnnotation(Upgrade.class) != null)) {
                            RoutingImpl rd = new RoutingImpl();
                            rd.clazz = cl;
                            rd.method = m;
                            if (!contracts.containsKey(namespace)) {
                                ContractInterface contract = (ContractInterface)rd.getContractClass().newInstance();
                                contracts.put(namespace, contract);
                            }
                            rd.contract = contracts.get(namespace);

                            if (m.getAnnotation(Transaction.class) != null) {
                                System.out.println("Found invoke method: " + m.getName());
                                if (m.getAnnotation(Transaction.class).submit()) {
                                    rd.type = Routing.TransactionType.INVOKE;
                                } else {
                                    rd.type = Routing.TransactionType.QUERY;
                                }

                            }
                            if (m.getAnnotation(Init.class) != null) {
                                rd.type = Routing.TransactionType.INIT;
                                System.out.println("Found init method: " + m.getName());
                            }
                            if (m.getAnnotation(Upgrade.class) != null) {
                                rd.type = Routing.TransactionType.UPGRADE;
                                System.out.println("Found init method: " + m.getName());
                            }
                            routingData.put(namespace + ":" + m.getName(), rd);
                        }
                    }
            }
        }
    }

    @Override
    public Routing getRouting(InvocationRequest req) {
        if (routingData.containsKey(req.getRequestName())) {
            return routingData.get(req.getRequestName());
        }
        return null;
    }

    @Override
    public Routing getDefaultRouting(InvocationRequest req) {
        if (contracts.containsKey(req.getNamespace())) {
            RoutingImpl tmpRoutingData = new RoutingImpl();
            tmpRoutingData.contract = contracts.get(req.getNamespace());
            try {
                tmpRoutingData.method = tmpRoutingData.contract.getClass().getMethod("unknownTransaction", null);
            } catch (NoSuchMethodException e) {

                return null;
            }
            tmpRoutingData.clazz = tmpRoutingData.contract.getClass();
            tmpRoutingData.type = Routing.TransactionType.DEFAULT;
            return tmpRoutingData;
        }
        return null;
    }
}
