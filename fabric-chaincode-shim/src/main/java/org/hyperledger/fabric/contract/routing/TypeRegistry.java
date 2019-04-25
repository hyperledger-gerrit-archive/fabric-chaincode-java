/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.contract.routing.impl.DataTypeDefinitionImpl;

/**
 * Registry to hold the complex data types as defined in the contract
 * Not used extensively at present but will have a great role when data handling comes up
 *
 */
public class TypeRegistry {

	private static Map<String, DataTypeDefinitionImpl> components = new HashMap<>();

	public static void addDataType(Class<?> cl) {
		DataTypeDefinitionImpl type = new DataTypeDefinitionImpl(cl);
		components.put(type.getName(), type);
	}

	public static Collection<DataTypeDefinitionImpl> getAllDataTypes() {
		return components.values();
	}

}
