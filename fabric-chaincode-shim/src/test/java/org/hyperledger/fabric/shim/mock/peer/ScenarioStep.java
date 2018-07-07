package org.hyperledger.fabric.shim.mock.peer;

import org.hyperledger.fabric.protos.peer.ChaincodeShim;

import java.util.List;

public interface ScenarioStep {
    boolean expected(ChaincodeShim.ChaincodeMessage msg);
    List<ChaincodeShim.ChaincodeMessage> next();
    long waitPeriod();
}
