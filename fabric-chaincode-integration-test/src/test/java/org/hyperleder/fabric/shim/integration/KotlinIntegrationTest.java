/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperleder.fabric.shim.integration;

import org.hamcrest.Matchers;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KotlinIntegrationTest {
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
    public void TestKotlinExampleChaincodeInstallInstantiateInvokeQuery() throws Exception {

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUser());

        Channel myChannel = Utils.getMyChannelBasicNetwork(client);

        // Installing chaincode
        InstallProposalRequest installProposalRequest = generateKotlinExampleInstallRequest(client);
        Utils.sendInstallProposals(client, installProposalRequest, myChannel.getPeers());

        // Instantiating chaincode
        InstantiateProposalRequest instantiateProposalRequest = generateKotlinExampleInstantiateRequest(client);
        Utils.sendInstantiateProposal("kotlincc", instantiateProposalRequest, myChannel, myChannel.getPeers(), myChannel.getOrderers());

        client.setUserContext(Utils.getUser1());

        // Send proposal and wait for responses
        TransactionProposalRequest proposalRequest = generateKotlinExampleInvokeRequest(client, "Alice", "Bob", "10");
        Utils.sendTransactionProposalInvoke(proposalRequest, myChannel, myChannel.getPeers(), myChannel.getOrderers());

        // Creating proposal for query
        TransactionProposalRequest queryAProposalRequest = generateKotlinExampleQueryRequest(client, "Alice");
        Utils.sendTransactionProposalQuery(queryAProposalRequest, myChannel, myChannel.getPeers(), Matchers.is(200), Matchers.is("90"), null);

        // Creating proposal for query
        TransactionProposalRequest queryBProposalRequest = generateKotlinExampleQueryRequest(client, "Bob");
        Utils.sendTransactionProposalQuery(queryBProposalRequest, myChannel, myChannel.getPeers(), Matchers.is(200), Matchers.is("210"), null);
    }

    static public InstallProposalRequest generateKotlinExampleInstallRequest(HFClient client) throws IOException, InvalidArgumentException {
        final InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        final ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("kotlincc")
                .setVersion("1.0")
                .build();


        final String chaincodeLocation = "../fabric-chaincode-example-kotlin";
        installProposalRequest.setChaincodeID(chaincodeID);
        installProposalRequest.setChaincodeLanguage(TransactionRequest.Type.JAVA);
        installProposalRequest.setChaincodeInputStream(Utils.generateTarGzInputStream(new File(chaincodeLocation), "src"));
        installProposalRequest.setChaincodeVersion("1.0");

        return installProposalRequest;
    }

    static public InstantiateProposalRequest generateKotlinExampleInstantiateRequest(HFClient client) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException {
        // Instantiating chaincode
        System.out.println("Instantiating chaincode: {Alice, 100, Bob, 200}");
        final ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("kotlincc")
                .setVersion("1.0")
                .build();

        // Building proposal
        System.out.println("Building instantiate proposal");
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(120000);
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setChaincodeLanguage(TransactionRequest.Type.JAVA);
        instantiateProposalRequest.setArgs(new String[]{"Alice", "100", "Bob", "200"});
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);

        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File("src/test/resources/chaincodeendorsementpolicy.yaml"));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        return instantiateProposalRequest;

    }

    static public TransactionProposalRequest generateKotlinExampleInvokeRequest(HFClient client, String key1, String key2, String value) {
        System.out.println("Creating proposal for set(" + key1 + ", " + key2 + ", " + value + ")");
        final TransactionProposalRequest proposalRequest = client.newTransactionProposalRequest();

        final ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("kotlincc")
                .setVersion("1.0")
                .build();

        proposalRequest.setChaincodeID(chaincodeID);
        proposalRequest.setFcn("invoke");
        proposalRequest.setProposalWaitTime(TimeUnit.SECONDS.toMillis(10));
        proposalRequest.setArgs(new String[]{key1, key2, value});

        return proposalRequest;

    }

    static public TransactionProposalRequest generateKotlinExampleQueryRequest(HFClient client, String key) {
        System.out.println("Creating proposal for get(" + key + ")");
        final TransactionProposalRequest queryProposalRequest = client.newTransactionProposalRequest();

        final ChaincodeID queryChaincodeID = ChaincodeID.newBuilder()
                .setName("kotlincc")
                .setVersion("1.0")
                .build();

        queryProposalRequest.setChaincodeID(queryChaincodeID);
        queryProposalRequest.setFcn("query");
        queryProposalRequest.setProposalWaitTime(TimeUnit.SECONDS.toMillis(10));
        queryProposalRequest.setArgs(new String[]{key});

        return queryProposalRequest;

    }


}
