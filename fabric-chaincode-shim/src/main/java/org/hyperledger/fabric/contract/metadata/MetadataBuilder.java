/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.hyperledger.fabric.Logger;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.routing.ContractDefinition;
import org.hyperledger.fabric.contract.routing.DataTypeDefinition;
import org.hyperledger.fabric.contract.routing.RoutingRegistry;
import org.hyperledger.fabric.contract.routing.TransactionType;
import org.hyperledger.fabric.contract.routing.TxFunction;
import org.hyperledger.fabric.contract.routing.TypeRegistry;
import org.hyperledger.fabric.contract.routing.impl.DataTypeDefinitionImpl;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.swagger.v3.oas.annotations.info.Info;

/**
 * Builder to assist in production of the metadata
 *
 * This class is used to build up the JSON structure to be returned as the metadata
 * It is not a store of information, rather a set of functional data to process to and from
 * metadata json to the internal data structure
 */
public class MetadataBuilder {
	private static Logger logger = Logger.getLogger(MetadataBuilder.class);

	// Custom sub-type of Map that helps with the case where if there's no value then do not
	// insert the property at all
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
	static Map<String, HashMap<String, Serializable>> contractMap = new HashMap<>();
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
	 * Setup the metadata from the found contracts
	 */
	public static void initialize() {
		Collection<ContractDefinition> contractDefinitions = RoutingRegistry.getAllDefinitions();
		contractDefinitions.forEach(MetadataBuilder::addContract);

		Collection<DataTypeDefinitionImpl> dataTypes = TypeRegistry.getAllDataTypes();
		dataTypes.forEach(MetadataBuilder::addComponent);

		// need to validate that the metadata that has been created is really valid
		// it should be as it's been created by code, but this is a valuable double
		// check
		logger.info("Validating scehma created");
		MetadataBuilder.validate();

	}

	/**
	 * Adds a component/ complex data-type
	 */
	public static void addComponent(DataTypeDefinition datatype) {

		Map<String, Object> component = new HashMap<>();

		component.put("$id", datatype.getName());
		component.put("type", "object");
		component.put("additionalProperties", false);
		component.put("properties", datatype.getProperties());
		componentMap.put(datatype.getSimpleName(), component);
	}

	/**
	 * Adds a new contract to the metadata as represented by the class object
	 *
	 * @param contractClass Class of the object to use as a contract
	 * @return the key that the contract class is referred to in the meteadata
	 */
	@SuppressWarnings("serial")
	public static String addContract(ContractDefinition contractDefinition) {
		Class<?> contractClass = contractDefinition.getContractImpl().getClass();

		Contract annotation = contractDefinition.getAnnotation();

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

		Collection<TxFunction> fns = contractDefinition.getTxFunctions();
		fns.forEach(txFn -> {
			MetadataBuilder.addTransaction(txFn, key);
		});

		return key;
	}

	/**
	 * Provide a mapping between the Java Language types and the OpenAPI based types
	 * @param clz
	 * @return
	 */
	public static Map<String, Object> propertySchema(Class<?> clz) {
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
	public static void addTransaction(TxFunction txFunction, String contractName) {
		Map<String, Object> transaction = new HashMap<String, Object>();
		Map<String, Object> returnSchema = propertySchema(txFunction.getReturnType());
		if (returnSchema != null) {
			transaction.put("returns", returnSchema);
		}

		ArrayList<TransactionType> tags = new ArrayList<TransactionType>();
		tags.add(txFunction.getType());

		Map<String, Serializable> contract = contractMap.get(contractName);
		@SuppressWarnings("unchecked")
		List<Object> txs = (ArrayList<Object>) contract.get("transactions");

		java.lang.reflect.Parameter[] params = txFunction.getParameters();
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
			transaction.put("name", txFunction.getName());
			txs.add(transaction);
		}
	}

	/**
	 * Returns the metadata as a JSON string (compact)
	 */
	public static String getMetadata() {
		return metadata().toString();
	}

	/**
	 * Returns the metadata as a JSON string (spaced out for humans)
	 */
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
