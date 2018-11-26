package org.hyperleder.fabric.shim.integration;

import org.hamcrest.Matchers;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeCollectionConfigurationException;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;


public class Example02NewModelIntegrationTest {
    @ClassRule
    public static DockerComposeContainer env = new DockerComposeContainer(
            new File("src/test/resources/basic-network/docker-compose.yml")
    )
            .withLocalCompose(false)
            .withPull(true);

    @BeforeClass
    public static void setUp() throws Exception {
        Utils.setUp();
    }

    @Test
    public void TestExample02ChaincodeInstallInstantiateInvokeQuery() throws Exception {

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUser());

        Channel myChannel = Utils.getMyChannelBasicNetwork(client);

        InstallProposalRequest installProposalRequest = generateExample02InstallRequest(client);
        Utils.sendInstallProposals(client, installProposalRequest, myChannel.getPeers());

        // Instantiating chaincode
        InstantiateProposalRequest instantiateProposalRequest = generateExample02InstantiateRequest(client);
        Utils.sendInstantiateProposal("newcc", instantiateProposalRequest, myChannel, myChannel.getPeers(), myChannel.getOrderers());

        client.setUserContext(Utils.getUser1());

        final TransactionProposalRequest proposalRequest = generateExample02InvokeRequest(client, "b", "a", "100");
        Utils.sendTransactionProposalInvoke(proposalRequest, myChannel, myChannel.getPeers(), myChannel.getOrderers());

        // Creating proposal for query
        final TransactionProposalRequest queryAProposalRequest = generateExample02QueryRequest(client, "a");
        Utils.sendTransactionProposalQuery(queryAProposalRequest, myChannel, myChannel.getPeers(), Matchers.is(200), Matchers.is("200"), null);

        // Creating proposal for query
        final TransactionProposalRequest queryBProposalRequest = generateExample02QueryRequest(client, "b");
        Utils.sendTransactionProposalQuery(queryBProposalRequest, myChannel, myChannel.getPeers(), Matchers.is(200), Matchers.is("100"), null);
    }

    static public InstallProposalRequest generateExample02InstallRequest(HFClient client) throws IOException, InvalidArgumentException {
        return Utils.generateInstallRequest(client, "newcc", "1.0", "../fabric-chaincode-example-gradle-new");
    }

    static public InstantiateProposalRequest generateExample02InstantiateRequest(HFClient client) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        return Utils.generateInstantiateRequest(client, "newcc", "1.0", "src/test/resources/chaincodeendorsementpolicy.yaml", null, "init", "a", "100", "b", "200");
    }

    static public TransactionProposalRequest generateExample02InvokeRequest(HFClient client, String key1, String key2, String value) {
        return Utils.generateTransactionRequest(client, "newcc", "1.0", "invoke", key1, key2, value);
    }

    static public TransactionProposalRequest generateExample02QueryRequest(HFClient client, String key) {
        return Utils.generateTransactionRequest(client, "newcc", "1.0", "query", key);
    }

}
