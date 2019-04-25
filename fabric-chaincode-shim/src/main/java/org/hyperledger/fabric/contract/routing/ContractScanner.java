/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract.routing;

/**
 * Scan and keep all chaincode requests -> contract routing information
 */
public interface ContractScanner {

    /**
     * Scan classpath for all contracts and build routing information for all contracts
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    void findAndSetContracts() throws IllegalAccessException, InstantiationException;

}
