/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Init;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.contract.execution.InvocationRequest;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import io.swagger.v3.oas.annotations.info.Info;
import org.everit.json.schema.ValidationException;

/**
 * Builder to assist in production of the metadata
 */
public class MetadataBuilder {
    private static Log logger = LogFactory.getLog(MetadataBuilder.class);

    // Specific Map that helps with the case where if there's no value then do not
    // insert
    // the
    @SuppressWarnings("serial")
    static class MetadataMap<K, V> extends HashMap<K, V> {

        V putIfNotNull(K key, V value) {
            logger.info(key + " " + value);
            if (value != null && !value.toString().isEmpty()) {
                return put(key, value);
            } else {
                return null;
            }
        }
    }

    // Metadata is composed of three primary sections
    // each of which is stored in a map
    static Map<String, Object> contractMap = new HashMap<String, Object>();
    static Map<String, Object> overallInfoMap = new HashMap<String, Object>();
    static Map<String, Object> componentMap = new HashMap<String, Object>();

    /**
     * Validation method
     *
     * @throws ValidationException if the metadata is not valid
     */
    public static void validate() {
        logger.info("Running schema test validation");
        try (InputStream inputStream = MetadataBuilder.class.getClassLoader()
                .getResourceAsStream("contract-schema.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(rawSchema);
            schema.validate(metadata());

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ValidationException e) {
            logger.error(e.getMessage());
            e.getCausingExceptions().stream().map(ValidationException::getMessage).forEach(logger::info);
            logger.error(debugString());
            throw e;
        }

    }

    /**
     * Adds a component/ complex datatype
     */
    public static void addComponent(Class<?> componentClass) {
        // given this class extract the property elements
        Field[] fields = componentClass.getDeclaredFields();
        Map<String, Object> component = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        for (Field f : fields) {
            Property propAnnotation = f.getAnnotation(Property.class);
            if (propAnnotation != null) {
                properties.put(f.getName(), propertySchema(f.getType()));
            }
        }
        component.put("$id", componentClass.getName());
        component.put("type", "object");
        component.put("additionalProperties", false);
        component.put("properties", properties);
        componentMap.put(componentClass.getSimpleName(), component);
    }

    /**
     * Adds a new contract to the metadata as represented by the class object
     *
     * @param contractClass Class of the object to use as a contract
     * @return the key that the contract class is referred to in the meteadata
     */
    @SuppressWarnings("serial")
    public static String addContract(Class<?> contractClass) {
        Contract annotation = contractClass.getAnnotation(Contract.class);
        String namespace = annotation.namespace();
        if (namespace == null || namespace.isEmpty()) {
            namespace = InvocationRequest.DEFAULT_NAMESPACE;
        }

        Info info = annotation.info();
        HashMap<String, Object> infoMap = new HashMap<String, Object>();
        infoMap.put("title", info.title());
        infoMap.put("description", info.description());
        infoMap.put("termsOfService", info.termsOfService());
        infoMap.put("contact", new MetadataMap<String, String>() {
            {
                putIfNotNull("email", info.contact().email());
                putIfNotNull("name", info.contact().name());
                putIfNotNull("url", info.contact().url());
            }
        });
        infoMap.put("license", new MetadataMap<String, String>() {
            {
                put("name", info.license().name());
                putIfNotNull("url", info.license().url());
            }
        });
        infoMap.put("version", info.version());

        String key = contractClass.getSimpleName();
        HashMap<String, Serializable> contract = new HashMap<String, Serializable>();
        contract.put("name", key);
        contract.put("transactions", new ArrayList<Object>());
        contract.put("info", infoMap);

        contractMap.put(key, contract);
        boolean defaultContract = true;
        if (defaultContract) {
            overallInfoMap.putAll(infoMap);
        }

        return key;
    }

    public static Map propertySchema(Class clz) {
        Map<String, Object> schema = new HashMap<String, Object>();
        String className = clz.getSimpleName();
        switch (className) {
        case "String":
            schema.put("type", className.toLowerCase());
            break;
        case "byte":
            schema.put("type", "integer");
            schema.put("format", "int8");
            break;
        case "short":
            schema.put("type", "integer");
            schema.put("format", "int16");
            break;
        case "int":
            schema.put("type", "integer");
            schema.put("format", "int32");
            break;
        case "long":
            schema.put("type", "integer");
            schema.put("format", "int64");
            break;
        case "double":
            schema.put("type", "number");
            schema.put("format", "double");
            break;
        case "float":
            schema.put("type", "number");
            schema.put("format", "float");
            break;
        case "boolean":
            schema.put("type", "boolean");
            break;
        default:
            return null;
        }

        return schema;
    }

    /**
     * Adds a new transaction function to the metadata for the given contract key
     *
     * @param method      Method object representing the transaction function
     * @param contractKey Key of the contract that this function belongs to
     */
    public static void addTransaction(Method method, String contractKey) {
        Map<String, Object> transaction = new HashMap<String, Object>();
        Map<String, Object> returnSchema = propertySchema(method.getReturnType());
        if (returnSchema != null) {
            transaction.put("returns", returnSchema);
        }

        ArrayList<TransactionType> tags = new ArrayList<TransactionType>();
        if (method.getAnnotation(Transaction.class) != null) {
            logger.debug("Found invoke method: " + method.getName());
            if (method.getAnnotation(Transaction.class).submit()) {
                tags.add(TransactionType.INVOKE);
            } else {
                tags.add(TransactionType.QUERY);
            }

        }
        if (method.getAnnotation(Init.class) != null) {
            tags.add(TransactionType.INIT);
            logger.debug("Found init method: " + method.getName());
        }

        Map contract = (Map) contractMap.get(contractKey);
        List<Object> txs = (ArrayList<Object>) contract.get("transactions");

        java.lang.reflect.Parameter[] params = method.getParameters();
        ArrayList<HashMap<String, Object>> paramsList = new ArrayList<HashMap<String, Object>>();

        for (java.lang.reflect.Parameter parameter : params) {
            HashMap<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put("name", parameter.getName());
            paramMap.put("schema", propertySchema(parameter.getType()));
            paramsList.add(paramMap);
        }

        transaction.put("parameters", paramsList);
        if (tags.size() != 0) {
            transaction.put("tags", tags.toArray());
            transaction.put("name", method.getName());
            txs.add(transaction);
        }
    }

    /** Returns the metadata as a JSON string (compact) */
    public static String getMetadata() {
        return metadata().toString();
    }

    /** Returns the metadata as a JSON string (spaced out for humans) */
    public static String debugString() {
        return metadata().toString(3);
    }

    /**
     * Create a JSONObject representing the schema
     *
     */
    private static JSONObject metadata() {
        HashMap<String, Object> metadata = new HashMap<String, Object>();

        metadata.put("$schema", "https://fabric-shim.github.io/contract-schema.json");
        metadata.put("info", overallInfoMap);
        metadata.put("contracts", contractMap);
        metadata.put("components", Collections.singletonMap("schemas", componentMap));

        JSONObject joMetadata = new JSONObject(metadata);
        return joMetadata;
    }

}
