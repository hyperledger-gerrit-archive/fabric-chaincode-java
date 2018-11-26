package org.hyperledger.fabric.example;

public abstract class ChaincodeBase extends org.hyperledger.fabric.shim.ChaincodeBase {

    public static Response newSuccessResponse(String message, byte[] payload) {
        return org.hyperledger.fabric.shim.ChaincodeBase.newSuccessResponse( message, payload);
    }

    public static Response newSuccessResponse() {
        return org.hyperledger.fabric.shim.ChaincodeBase.newSuccessResponse(null, null);
    }

    public static Response newSuccessResponse(String message) {
        return org.hyperledger.fabric.shim.ChaincodeBase.newSuccessResponse(message, null);
    }

    public static Response newSuccessResponse(byte[] payload) {
        return org.hyperledger.fabric.shim.ChaincodeBase.newSuccessResponse(null, payload);
    }

    public static Response newErrorResponse() {
        return org.hyperledger.fabric.shim.ChaincodeBase.newErrorResponse(null, null);
    }

    public static Response newErrorResponse(String message) {
        return org.hyperledger.fabric.shim.ChaincodeBase.newErrorResponse(message, null);
    }

    public static Response newErrorResponse(byte[] payload) {
        return org.hyperledger.fabric.shim.ChaincodeBase.newErrorResponse(null, payload);
    }

    public static Response newErrorResponse(Throwable throwable) {
        return org.hyperledger.fabric.shim.ChaincodeBase.newErrorResponse(throwable);
    }

}
