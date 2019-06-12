/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing.impl;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperledger.fabric.Logger;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * Registry to hold permit access to the routing definitions. This is the
 * primary internal data structure to permit access to information about the
 * contracts, and their transaction functions.
 *
 * Contracts are added, and processed. At runtime, this can then be accessed to
 * locate a specific 'Route' that can be handed off to the ExecutionService
 *
 */
public class RegistryImpl<T extends RegistryImpl.Entry, A extends Annotation> /* implements RoutingRegistry */ {
    private static Logger logger = Logger.getLogger(RegistryImpl.class);

    public interface Entry {

        String getName();

    }

    private Class<A> annotationClass;

    public RegistryImpl(Class<A> annotationClass) {
        this.annotationClass = annotationClass;
    }

    private Map<String, T> contents = new HashMap<>();

    /*
     * (non-Javadoc)
     *
     * @see
     * org.hyperledger.fabric.contract.routing.RoutingRegistry#getAllDefinitions()
     */
    public Collection<T> getAllDefinitions() {
        return contents.values();
    }

    public T addNew(Class<T> clz) throws InstantiationException, IllegalAccessException {
        logger.debug(() -> "Adding new Class " + clz.getCanonicalName());
        T newObj = clz.newInstance();
        contents.put(newObj.getName(), newObj);
        return newObj;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.hyperledger.fabric.contract.routing.RoutingRegistry#findAndSetContracts()
     */

    public void findAndSetContents() throws InstantiationException, IllegalAccessException {
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
        for (Class<?> cl : ref.getTypesAnnotatedWith(annotationClass)) {
            logger.info("Found class: " + cl.getCanonicalName());
            if (RegistryImpl.Entry.class.isAssignableFrom(cl)) {
                logger.debug("Inheritance ok");
                String className = cl.getCanonicalName();
                if (!seenClass.contains(className)) {
                    T entry = addNew((Class<T>) cl);
                    seenClass.add(className);
                }
            } else {
                logger.debug("Class is not assignabled from Contract");
            }
        }

    }

    public T get(String string) {
        return contents.get(string);
    }

}
