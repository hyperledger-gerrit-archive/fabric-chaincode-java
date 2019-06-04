/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.contract.metadata.TypeSchema;
import org.hyperledger.fabric.contract.routing.DataTypeDefinition;
import org.hyperledger.fabric.contract.routing.PropertyDefinition;

public class DataTypeDefinitionImpl implements DataTypeDefinition {

	List<PropertyDefinition> properties = new ArrayList<>();
	Map<String, Field> fields = new HashMap<>();
	String name;
	String simpleName;
	Class<?> clazz;

	public DataTypeDefinitionImpl(Class<?> componentClass) {
		this.clazz = componentClass;
		this.name = componentClass.getName();
		this.simpleName = componentClass.getSimpleName();
		// given this class extract the property elements
		Field[] fields = componentClass.getDeclaredFields();

		for (Field f : fields) {
			Property propAnnotation = f.getAnnotation(Property.class);
			if (propAnnotation != null) {
				PropertyDefinition propDef = new PropertyDefinitionImpl(f.getName(), f.getClass(),
						TypeSchema.typeConvert(f.getType()), f);
				this.properties.add(propDef);
			}
		}
	}

	public Class<?> getTypeClass() {
		return this.clazz;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.hyperledger.fabric.contract.routing.DataTypeDefinition#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hyperledger.fabric.contract.routing.DataTypeDefinition#getProperties()
	 */
	@Override
	public List<PropertyDefinition> getProperties() {
		return properties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.hyperledger.fabric.contract.routing.DataTypeDefinition#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return simpleName;
	}

}
