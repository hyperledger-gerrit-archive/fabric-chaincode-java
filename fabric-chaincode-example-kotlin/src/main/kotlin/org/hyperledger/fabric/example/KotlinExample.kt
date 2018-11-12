/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.example

import com.google.protobuf.ByteString
import org.apache.commons.logging.LogFactory
import org.hyperledger.fabric.shim.Chaincode
import org.hyperledger.fabric.shim.ChaincodeBase
import org.hyperledger.fabric.shim.ChaincodeStub
import java.nio.charset.StandardCharsets.UTF_8;

class KotlinExample : ChaincodeBase() {

    companion object {
        val _logger = LogFactory.getLog(KotlinExample::class.java)
    }
    override fun init(stub: ChaincodeStub?): Chaincode.Response {

        try {
            _logger.info("Init java simple chaincode")
            val function = stub?.getFunction()
            if (!function.equals("init")) {
                return newErrorResponse("function other than init is not supported")
            }
            val args = stub?.getParameters()
            if (args?.size != 4) {
                return newErrorResponse("Incorrect number of arguments. Expecting 4")
            }

            _logger.info(String.format("account %s, value = %s; account %s, value %s", args[0], args[1], args[2], args[3]))
            stub?.putStringState(args[0], args[1])
            stub?.putStringState(args[2], args[3])

            return newSuccessResponse()
        } catch(e:Throwable) {
            return newErrorResponse(e.message)
        }
    }

    override fun invoke(stub: ChaincodeStub?): Chaincode.Response {
        try {
            _logger.info("Invoke java simple chaincode")
            val func = stub?.getFunction()
            val params = stub?.getParameters()
            if (func == "invoke") {
                return invoke(stub, params as List<String>)
            }
            if (func == "delete") {
                return delete(stub, params as List<String>)
            }
            return if (func == "query") {
                query(stub, params as List<String>)
            } else ChaincodeBase.newErrorResponse("Invalid invoke function name. Expecting one of: [\"invoke\", \"delete\", \"query\"]")
        } catch (e: Throwable) {
            return ChaincodeBase.newErrorResponse(e)
        }

    }

    private fun invoke(stub: ChaincodeStub?, args: List<String>): Chaincode.Response {
        if (args.size != 3) {
            return newErrorResponse("Incorrect number of arguments. Expecting 3")
        }
        val accountFromKey = args[0]
        val accountToKey = args[1]

        val accountFromValueStr = stub?.getStringState(accountFromKey)
                ?: return ChaincodeBase.newErrorResponse(String.format("Entity %s not found", accountFromKey))
        var accountFromValue = Integer.parseInt(accountFromValueStr)

        val accountToValueStr = stub?.getStringState(accountToKey)
                ?: return ChaincodeBase.newErrorResponse(String.format("Entity %s not found", accountToKey))
        var accountToValue = Integer.parseInt(accountToValueStr)

        val amount = Integer.parseInt(args[2])

        if (amount > accountFromValue) {
            return newErrorResponse(String.format("not enough money in account %s", accountFromKey))
        }

        accountFromValue -= amount
        accountToValue += amount

        _logger.info(String.format("new value of A: %s", accountFromValue))
        _logger.info(String.format("new value of B: %s", accountToValue))

        stub?.putStringState(accountFromKey, Integer.toString(accountFromValue))
        stub?.putStringState(accountToKey, Integer.toString(accountToValue))

        _logger.info("Transfer complete")

        return newSuccessResponse("invoke finished successfully", ByteString.copyFrom("$accountFromKey: $accountFromValue $accountToKey: $accountToValue", UTF_8).toByteArray())
    }

    // Deletes an entity from state
    private fun delete(stub: ChaincodeStub?, args: List<String>): Chaincode.Response {
        if (args.size != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting 1")
        }
        val key = args[0]
        // Delete the key from the state in ledger
        stub?.delState(key)
        return newSuccessResponse()
    }


    // query callback representing the query of a chaincode
    private fun query(stub: ChaincodeStub?, args: List<String>): Chaincode.Response {
        if (args.size != 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting name of the person to query")
        }
        val key = args[0]
        //byte[] stateBytes
        val v = stub?.getStringState(key)
                ?: return newErrorResponse(String.format("Error: state for %s is null", key))
        _logger.info(String.format("Query Response:\nName: %s, Amount: %s\n", key, v))
        return newSuccessResponse(v, ByteString.copyFrom(v, UTF_8).toByteArray())
    }

}

fun main(args:Array<String>) {
    val kotlinExample = KotlinExample()
    kotlinExample.start(args)
}