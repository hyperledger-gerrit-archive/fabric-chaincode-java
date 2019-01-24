/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contact.routing;

import org.hyperledger.fabric.contact.ContractInterface;
import org.hyperledger.fabric.contact.annotation.Contract;
import org.hyperledger.fabric.contact.annotation.Init;
import org.hyperledger.fabric.contact.annotation.Transaction;
import org.hyperledger.fabric.contact.annotation.Upgrade;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class ContractScanner {
    public  static final String DEFAULT_NAMESPACE = "default";
    Map<String, RoutingData> routingData = new HashMap<>();
    Map<String, Object> chaincodes = new HashMap<>();

    public void findAndSetChaincode() throws IllegalAccessException, InstantiationException {
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
                namespace = DEFAULT_NAMESPACE;
            }
            System.out.println("Found class: " + cl.getCanonicalName());
            System.out.println("Searching init and invoke annotated methods");
            if (ContractInterface.class.isAssignableFrom(cl)) {
                if (chaincodes.containsKey(cl.getName()))
                    for (Method m : cl.getMethods()) {
                        if ((m.getAnnotation(Transaction.class) != null) || (m.getAnnotation(Init.class) != null) || (m.getAnnotation(Upgrade.class) != null)) {
                            RoutingData rd = new RoutingData();
                            rd.clazz = cl;
                            rd.method = m;
                            if (!chaincodes.containsKey(namespace)) {
                                Object chaincode = cl.newInstance();
                                chaincodes.put(namespace, chaincode);
                            }
                            rd.chaincode = chaincodes.get(namespace);

                            if (m.getAnnotation(Transaction.class) != null) {
                                System.out.println("Found invoke method: " + m.getName());
                                if (m.getAnnotation(Transaction.class).submit()) {
                                    rd.type = RoutingData.TransactionType.INVOKE;
                                } else {
                                    rd.type = RoutingData.TransactionType.QUERY;
                                }

                            }
                            if (m.getAnnotation(Init.class) != null) {
                                rd.type = RoutingData.TransactionType.INIT;
                                System.out.println("Found init method: " + m.getName());
                            }
                            if (m.getAnnotation(Upgrade.class) != null) {
                                rd.type = RoutingData.TransactionType.UPGRADE;
                                System.out.println("Found init method: " + m.getName());
                            }
                            routingData.put(namespace + ":" + m.getName(), rd);
                        }
                    }
            }
        }
    }

    public RoutingData getRouting(ChaincodeStub stub) {
        String func = stub.getFunction();
        String funcParts[] = func.split(":");

        String namespace = "";
        String methodName;

        if (funcParts.length == 2) {
            namespace = funcParts[0];
            methodName = funcParts[1];
        } else {
            namespace = DEFAULT_NAMESPACE;
            methodName = funcParts[1];
        }

        if (routingData.containsKey(namespace + ":" + methodName)) {
            return routingData.get(namespace + ":" + methodName);
        }

        return null;
    }

}
