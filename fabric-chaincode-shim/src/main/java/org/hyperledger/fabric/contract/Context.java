/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.contract.ClientIdentity;

/**
 *
 * This context is available to all 'transaction functions' and provides the
 * transaction context. It also provides access to the APIs for the world state
 * using {@link #getStub()}
 * <p>
 * Applications can implement their own versions if they wish to add
 * functionality. All subclasses MUST implement a constructor, for example
 * <pre>
 * {@code
 *
 * public MyContext extends Context {
 *
 *     public MyContext(ChaincodeStub stub) {
 *        super(stub);
 *     }
 * }
 *
 *}
 *</pre>
 *
 */
public class Context {
    protected ChaincodeStub stub;
    protected ClientIdentity clientIdentity;

    /**
     * Constructor
     * @param stub Instance of the {@link ChaincodeStub} to use
     */
    public Context(ChaincodeStub stub) {
        this.stub = stub;
    }

    /**
     *
     * @return ChaincodeStub instance to use
     */
    public ChaincodeStub getStub() {
        return this.stub;
    }

    /**
	 * This sets the ClientIdentity object to use for information on the transaction invoking identity
	 * MUST NOT BE CALLED FROM SMART CONTRACT CODE
	 *
	 * @param ClientIdentity clientIdentity chaincode stub instance
	 */
    public void setClientIdentity(ClientIdentity clientIdentity) {
        this.clientIdentity = clientIdentity;
    }

    /**
     *
     * @return ClientIdentity object to use
     */
    public ClientIdentity getClientIdentity() {
        return this.clientIdentity;
    }
}
