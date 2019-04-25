/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.contract.simplepath;

import static org.hamcrest.Matchers.is;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.COMPLETED;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.ERROR;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.READY;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.REGISTER;
import static org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage.Type.TRANSACTION;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hyperledger.fabric.contract.ContractRouter;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInput.Builder;
import org.hyperledger.fabric.protos.peer.ChaincodeShim;
import org.hyperledger.fabric.protos.peer.ChaincodeShim.ChaincodeMessage;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage;
import org.hyperledger.fabric.protos.peer.ProposalResponsePackage.Response;
import org.hyperledger.fabric.shim.mock.peer.ChaincodeMockPeer;
import org.hyperledger.fabric.shim.mock.peer.CompleteStep;
import org.hyperledger.fabric.shim.mock.peer.ErrorResponseStep;
import org.hyperledger.fabric.shim.mock.peer.RegisterStep;
import org.hyperledger.fabric.shim.mock.peer.ScenarioStep;
import org.hyperledger.fabric.shim.utils.MessageUtil;
import org.hyperledger.fabric.shim.utils.TimeoutUtil;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;

import com.google.protobuf.ByteString;

public class ContractSimplePath {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    ChaincodeMockPeer server;

    @After
    public void afterTest() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Test starting the contract logic 
     * @throws Exception
     */
    @Test
    public void testContract() throws Exception {
        

        List<ScenarioStep> scenario = new ArrayList<>();
        scenario.add(new RegisterStep());
        setLogLevel("DEBUG");
        server = ChaincodeMockPeer.startServer(scenario);
        ContractRouter.main(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});

        checkScenarioStepEnded(server, 1, 5000, TimeUnit.MILLISECONDS);

        assertThat(server.getLastMessageSend().getType(), is(READY));
        assertThat(server.getLastMessageRcvd().getType(), is(REGISTER));
    }

    /**
     * Test executing two transaction functions in a contract via fully qualified name
     * @throws Exception
     */
    @Test
    public void main() throws Exception {
    	
        List<ScenarioStep> scenario = new ArrayList<>();
        scenario.add(new RegisterStep());
        scenario.add(new CompleteStep());
        scenario.add(new CompleteStep());

        setLogLevel("DEBUG");
        server = ChaincodeMockPeer.startServer(scenario);       
     
        ContractRouter.main(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});
        checkScenarioStepEnded(server, 1, 5000, TimeUnit.MILLISECONDS);        

        ChaincodeShim.ChaincodeMessage initMsg = newInvokeFn(new String[] { "samplecontract:i1" });
        server.send(initMsg);      
        checkScenarioStepEnded(server, 2, 5000, TimeUnit.MILLISECONDS);       
        assertThat(server.getLastMessageRcvd().getType(), is(COMPLETED));
        assertThat(getLastReturnString(), is("Init done"));        
                
        ChaincodeShim.ChaincodeMessage invokeMsg = newInvokeFn(new String[] { "samplecontract:t1","a" }); 
        server.send(invokeMsg);
        checkScenarioStepEnded(server, 3, 5000, TimeUnit.MILLISECONDS);     
        assertThat(server.getLastMessageRcvd().getType(), is(COMPLETED));
        assertThat(getLastReturnString(), is("a")); 
    }
    
    /**
    * Test executing two transaction functions in a contract via default name
    * @throws Exception
    */
   @Test
   public void defaultNamespace() throws Exception {
   	
       List<ScenarioStep> scenario = new ArrayList<>();
       scenario.add(new RegisterStep());
       scenario.add(new CompleteStep());
       scenario.add(new CompleteStep());

       setLogLevel("DEBUG");
       server = ChaincodeMockPeer.startServer(scenario);       
    
       ContractRouter.main(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});
       checkScenarioStepEnded(server, 1, 5000, TimeUnit.MILLISECONDS);        

       ChaincodeShim.ChaincodeMessage initMsg = newInvokeFn(new String[] { "i1" });
       server.send(initMsg);      
       checkScenarioStepEnded(server, 2, 5000, TimeUnit.MILLISECONDS);       
       assertThat(server.getLastMessageRcvd().getType(), is(COMPLETED));
       assertThat(getLastReturnString(), is("Init done"));        
               
       ChaincodeShim.ChaincodeMessage invokeMsg = newInvokeFn(new String[] { "t1","a" }); 
       server.send(invokeMsg);
       checkScenarioStepEnded(server, 3, 5000, TimeUnit.MILLISECONDS);     
       assertThat(server.getLastMessageRcvd().getType(), is(COMPLETED));
       assertThat(getLastReturnString(), is("a")); 
   }
   
   /**
   * Test executing two a function that does not exist
   * @throws Exception
   */
  @Test
  public void unkownFn() throws Exception {
  	
      List<ScenarioStep> scenario = new ArrayList<>();
      scenario.add(new RegisterStep());
      scenario.add(new ErrorResponseStep());
      
      setLogLevel("DEBUG");
      server = ChaincodeMockPeer.startServer(scenario);       
   
      ContractRouter.main(new String[]{"-a", "127.0.0.1:7052", "-i", "testId"});
      checkScenarioStepEnded(server, 1, 5000, TimeUnit.MILLISECONDS);        

      ChaincodeShim.ChaincodeMessage initMsg = newInvokeFn(new String[] { "samplecontract:wibble" });
      server.send(initMsg);      
      checkScenarioStepEnded(server, 2, 5000, TimeUnit.MILLISECONDS);       
      assertThat(server.getLastMessageRcvd().getType(), is(ERROR));
      assertThat(server.getLastMessageRcvd().getPayload().toStringUtf8(), is("Undefined contract method called"));   
              
 
  }
    
    public static void checkScenarioStepEnded(final ChaincodeMockPeer s, final int step, final int timeout, final TimeUnit units) throws Exception {
        try {
            TimeoutUtil.runWithTimeout(new Thread(() -> {
                while (true) {
                    if (s.getLastExecutedStep() == step) return;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
            }), timeout, units);
        } catch (TimeoutException e) {
            fail("Got timeout, step " + step + " not finished");
        }
    }
    
    
    public ChaincodeMessage newInvokeFn(String args[]) {
        Builder invokePayload = Chaincode.ChaincodeInput.newBuilder();
        for (String arg : args) {
			invokePayload.addArgs(ByteString.copyFromUtf8(arg));
		}

        return MessageUtil.newEventMessage(TRANSACTION, "testChannel", "0", invokePayload.build().toByteString(), null);
    }
    public String getLastReturnString() throws Exception {
    	Response resp = ProposalResponsePackage.Response.parseFrom(server.getLastMessageRcvd().getPayload());
        return (resp.getPayload().toStringUtf8());
    }
    
    public void setLogLevel(String logLevel) {
        environmentVariables.set("CORE_CHAINCODE_LOGGING_SHIM", logLevel);
        environmentVariables.set("CORE_CHAINCODE_LOGGING_LEVEL", logLevel);
    }
}
