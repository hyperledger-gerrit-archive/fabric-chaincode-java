/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.routing.impl;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Init;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.execution.InvocationRequest;
import org.hyperledger.fabric.contract.metadata.MetadataBuilder;
import org.hyperledger.fabric.contract.routing.ContractDefinition;
import org.hyperledger.fabric.contract.routing.ContractScanner;
import org.hyperledger.fabric.contract.routing.Routing;
import org.hyperledger.fabric.contract.routing.RoutingRegistry;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.hyperledger.fabric.contract.routing.TypeRegistry;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class ContractScannerImpl implements ContractScanner {
    private static Logger logger = Logger.getLogger(ContractScannerImpl.class);

    @Override
    public void findAndSetContracts() {
        ArrayList<URL> urls = new ArrayList<>();
        ClassLoader[] classloaders = { getClass().getClassLoader(), Thread.currentThread().getContextClassLoader() };
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
        Reflections ref = new Reflections(configurationBuilder);

        logger.info("Searching chaincode class in urls: " + urls);

        // set to ensure that we don't scan the same class twice
        Set<String> seenClass = new HashSet<>();

        // loop over all the classes that have the Contract annotation
        for (Class<?> cl : ref.getTypesAnnotatedWith(Contract.class)) {
            logger.info("Found class: " + cl.getCanonicalName());
            if (ContractInterface.class.isAssignableFrom(cl)) {
                logger.debug("Inheritance ok");
                String className = cl.getCanonicalName();

                if (!seenClass.contains(className)) {
                	ContractDefinition contract = RoutingRegistry.addNewContract(cl);

                	logger.debug("Searching init and invoke annotated methods");
                    for (Method m : cl.getMethods()) {
                        if ((m.getAnnotation(Transaction.class) != null) || (m.getAnnotation(Init.class) != null)) {
                            logger.debug("Found annotated method " + m.getName());

                            contract.addTxFunction(m);

                        }
                    }

                    seenClass.add(className);
                }
            } else {
                logger.debug("Class is not assignabled from Contract");
            }
        }

        // now need to look for the data types have been set with the
        logger.info("Looking for the data types");
        Set<Class<?>> czs = ref.getTypesAnnotatedWith(DataType.class);
        logger.info("found "+czs.size());
        czs.forEach(TypeRegistry::addDataType);

    }

}
