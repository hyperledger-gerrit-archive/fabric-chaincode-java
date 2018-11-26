package org.hyperledger.fabric.example;

import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.shim.Chaincode.Response;
import org.hyperledger.fabric.shim.ChaincodeClass;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.Init;
import org.hyperledger.fabric.shim.Invoke;

import static java.nio.charset.StandardCharsets.UTF_8;

@ChaincodeClass
public class SimpleChaincode {

    private static Log _logger = LogFactory.getLog(SimpleChaincode.class);

    @Init
    public Response init(ChaincodeStub stub, String account1Key, String account1Value, String account2Key, String account2Value) {
        try {
            stub.putStringState(account1Key, account1Value);
            stub.putStringState(account2Key, account2Value);
            return ChaincodeBase.newSuccessResponse();
        } catch ( Throwable e) {
            return ChaincodeBase.newErrorResponse(e);
        }
    }

    @Invoke
    public Response invoke(ChaincodeStub stub, String accountFromKey, String accountToKey, String amountStr) {
        try {
            String accountFromValueStr = stub.getStringState(accountFromKey);
            if (accountFromValueStr == null) {
                return ChaincodeBase.newErrorResponse(String.format("Entity %s not found", accountFromKey));
            }
            int accountFromValue = Integer.parseInt(accountFromValueStr);

            String accountToValueStr = stub.getStringState(accountToKey);
            if (accountToValueStr == null) {
                return ChaincodeBase.newErrorResponse(String.format("Entity %s not found", accountToKey));
            }
            int accountToValue = Integer.parseInt(accountToValueStr);

            int amount = Integer.parseInt(amountStr);

            if (amount > accountFromValue) {
                return ChaincodeBase.newErrorResponse(String.format("not enough money in account %s", accountFromKey));
            }

            accountFromValue -= amount;
            accountToValue += amount;

            _logger.info(String.format("new value of A: %s", accountFromValue));
            _logger.info(String.format("new value of B: %s", accountToValue));

            stub.putStringState(accountFromKey, Integer.toString(accountFromValue));
            stub.putStringState(accountToKey, Integer.toString(accountToValue));

            _logger.info("Transfer complete");

            return ChaincodeBase.newSuccessResponse("invoke finished successfully", ByteString.copyFrom(accountFromKey + ": " + accountFromValue + " " + accountToKey + ": " + accountToValue, UTF_8).toByteArray());
        } catch (Throwable e) {
            return ChaincodeBase.newErrorResponse(e);
        }
    }

    @Invoke
    public Response delete(ChaincodeStub stub, String key) {
        try {
            stub.delState(key);
            return ChaincodeBase.newSuccessResponse();
        } catch (Throwable e) {
            return ChaincodeBase.newErrorResponse(e);
        }
    }

    @Invoke
    public Response set(ChaincodeStub stub, String key, String value) {
        stub.putStringState(key,value);
        return ChaincodeBase.newSuccessResponse();
    }

    // query callback representing the query of a chaincode
    @Invoke
    public Response query(ChaincodeStub stub, String key) {
        try {
            //byte[] stateBytes
            String val	= stub.getStringState(key);
            if (val == null) {
                return ChaincodeBase.newErrorResponse(String.format("Error: state for %s is null", key));
            }
            _logger.info(String.format("Query Response:\nName: %s, Amount: %s\n", key, val));
            return ChaincodeBase.newSuccessResponse(val, ByteString.copyFrom(val, UTF_8).toByteArray());
        } catch (Throwable e) {
            return ChaincodeBase.newErrorResponse(e);
        }
    }

}
