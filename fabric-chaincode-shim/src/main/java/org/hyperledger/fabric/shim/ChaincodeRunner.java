/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.shim;

import io.netty.handler.ssl.OpenSsl;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChaincodeRunner extends ChaincodeBase {

    Object chaincode;
    Map<String, Method> invokeMethods = new HashMap<>();
    Method initMethod;

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

        for (Class<?> cl : ref.getTypesAnnotatedWith(ChaincodeClass.class)) {
            System.out.println("Found class: " + cl.getCanonicalName());
            System.out.println("Searching init and invoke annotated methods");
            boolean initFound = false;
            boolean invokeFound = false;
            for (Method m : cl.getMethods()) {
                if (m.getAnnotation(Invoke.class) != null) {
                    invokeFound = true;
                    System.out.println("Found invoke method: " + m.getName());
                    invokeMethods.put(m.getName(), m);
                }
                if (m.getAnnotation(Init.class) != null) {
                    initFound = true;
                    System.out.println("Found init method: " + m.getName());
                    initMethod = m;
                }
            }
            if (initFound && invokeFound) {
                chaincode = cl.newInstance();
                System.out.println("Class " + cl.getCanonicalName() + " correctly annotated and have init and invoke methods");
                return;
            } else {
                System.out.println("Class " + cl.getCanonicalName() + " correctly annotated, but no init/invoke methods found");
                initMethod = null;
                invokeMethods.clear();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("OpenSSL avaliable: " + OpenSsl.isAvailable());

        ChaincodeRunner r = new ChaincodeRunner();
        try {
            r.findAndSetChaincode();
            if (r.chaincode != null) {
                r.start(args);
            } else {
                System.err.println("No chaincode class found");
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response init(ChaincodeStub stub) {
        List<byte[]> stubArgs = stub.getArgs();
        Class<?>[] initMethodParameterTypes = initMethod.getParameterTypes();
        List<Object> initArgs = new ArrayList<>();

        initArgs.add(stub);
        initArgs.addAll(convertArgs(stubArgs, initMethodParameterTypes));

        try {
            System.out.println("Calling to init method " + initMethod.getName() + "(" + String.join(", ", initArgs.stream().map(a -> a.toString()).collect(Collectors.toList())));
            return (Chaincode.Response)initMethod.invoke(chaincode, initArgs.toArray());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        List<byte[]> stubArgs = stub.getArgs();
        String func = stub.getFunction();
        if (invokeMethods.containsKey(func)) {
            Method invokeMethod = invokeMethods.get(func);
            Class<?>[] invokeMethodParameterTypes = invokeMethod.getParameterTypes();
            List<Object> invokeArgs = new ArrayList<>();

            invokeArgs.add(stub);
            invokeArgs.addAll(convertArgs(stubArgs, invokeMethodParameterTypes));

            try {
                return (Chaincode.Response)invokeMethod.invoke(chaincode, invokeArgs.toArray());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }
        return null;
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
}
