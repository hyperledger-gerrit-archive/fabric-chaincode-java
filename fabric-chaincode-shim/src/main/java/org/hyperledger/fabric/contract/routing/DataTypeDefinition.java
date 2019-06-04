/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.routing;

import java.util.List;

public interface DataTypeDefinition {

	String getName();

	List<PropertyDefinition> getProperties();

	String getSimpleName();
	
    Class<?> getTypeClass();
}