/*
Copyright IBM Corp. All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/
package org.hyperleder.fabric.shim.integration;

import com.google.protobuf.ByteString;
import org.hamcrest.Matchers;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class FirstNetworkIntegrationTest {

    @ClassRule
    public static DockerComposeContainer env = new DockerComposeContainer(
            new File("src/test/resources/first-network/docker-compose-cli.yaml")
    )
            .withLocalCompose(false)
            .withPull(false);

    @BeforeClass
    public static void setUp() throws Exception {
        Utils.setUp();
    }

    @AfterClass
    public static void shutDown() throws Exception {
        Utils.removeDevContainerAndImages();
    }

    @Test(timeout = 120000)
    public void TestNoBuildChaincodeInstallInstantiateWithSrc() throws Exception {

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUserOrg1TLS());

        Channel myChannel = Utils.getMyChannelFirstNetwork(client);

        List<Peer> peer0org1 = Utils.getPeersFromChannel(myChannel, "peer0.org1");

        InstallProposalRequest installProposalRequest = generateNoBuildInstallRequest(client, "nobuildcc", true);
        Utils.sendInstallProposals(client, installProposalRequest, peer0org1);

        // Instantiating chaincode
        InstantiateProposalRequest instantiateProposalRequest = generateInstantiateRequest(client, "nobuildcc");
        ProposalResponse response = Utils.sendInstantiateProposalReturnFaulureResponse("nobuildcc", instantiateProposalRequest, myChannel, peer0org1, myChannel.getOrderers());

        assertThat(response.getMessage(), containsString("Not build.gralde nor pom.xml found in chaincode source, don't know how to build chaincode"));

        assertThat(response.getMessage(), containsString("/chaincode/input/src/src/main"));
    }

    @Test(timeout = 120000)
    public void TestNoBuildChaincodeInstallInstantiateWithoutSrc() throws Exception {

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUserOrg1TLS());

        Channel myChannel = Utils.getMyChannelFirstNetwork(client);

        List<Peer> peer0org1 = Utils.getPeersFromChannel(myChannel, "peer0.org1");

        InstallProposalRequest installProposalRequest = generateNoBuildInstallRequest(client, "nobuildcc2", false);
        Utils.sendInstallProposals(client, installProposalRequest, peer0org1);

        // Instantiating chaincode
        InstantiateProposalRequest instantiateProposalRequest = generateInstantiateRequest(client, "nobuildcc2");
        ProposalResponse response = Utils.sendInstantiateProposalReturnFaulureResponse("nobuildcc2", instantiateProposalRequest, myChannel, peer0org1, myChannel.getOrderers());

        assertThat(response.getMessage(), containsString("Not build.gralde nor pom.xml found in chaincode source, don't know how to build chaincode"));

        assertThat(response.getMessage(), containsString("/chaincode/input/src/main"));
    }

    @Test(timeout = 120000)
    public void TestNoMainChaincodeInstallInstantiate() throws Exception {

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUserOrg1TLS());

        Channel myChannel = Utils.getMyChannelFirstNetwork(client);

        InstallProposalRequest installProposalRequest = generateNoMainInstallRequest(client, "nomaincc", true);
        Utils.sendInstallProposals(client, installProposalRequest, myChannel.getPeers().stream().filter(peer -> peer.getName().indexOf("org1") != -1).collect(Collectors.toList()));

        // Instantiating chaincode
        List<Peer> peer0org1 = myChannel.getPeers().stream().filter(peer -> peer.getName().indexOf("peer0.org1") != -1).collect(Collectors.toList());
        InstantiateProposalRequest instantiateProposalRequest = generateInstantiateRequest(client, "nomaincc");
        ProposalResponse response = Utils.sendInstantiateProposalReturnFaulureResponse("nomaincc", instantiateProposalRequest, myChannel, peer0org1, myChannel.getOrderers());

        assertThat(response.getMessage(), containsString("chaincode registration failed: container exited with 1"));
    }


    @Test
    public void TestSACCChaincodeInstallInstantiateInvokeQuery() throws Exception {

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUserOrg1TLS());

        Channel myChannel = Utils.getMyChannelFirstNetwork(client);

        List<Peer> peer0org1 = Utils.getPeersFromChannel(myChannel, "peer0.org1");
        List<Peer> peer1org1 = Utils.getPeersFromChannel(myChannel, "peer1.org1");
        List<Peer> org1peers = Utils.getPeersFromChannel(myChannel, "org1");

        InstallProposalRequest installProposalRequest = generateSACCInstallRequest(client);
        Utils.sendInstallProposals(client, installProposalRequest,  org1peers);

        // Instantiating chaincode
        InstantiateProposalRequest instantiateProposalRequest = generateInstantiateRequest(client, "sacc");
        Utils.sendInstantiateProposal("sacc", instantiateProposalRequest, myChannel, peer0org1, myChannel.getOrderers());

        client.setUserContext(Utils.getUser1Org1TLS());

        final TransactionProposalRequest proposalRequest = generateSACCInvokeRequest(client, "b", "200");
        Utils.sendTransactionProposalInvoke(proposalRequest, myChannel, peer0org1, myChannel.getOrderers());

        // Creating proposal for query
        final TransactionProposalRequest queryAProposalRequest = generateSACCQueryRequest(client, "a");
        Utils.sendTransactionProposalQuery(queryAProposalRequest, myChannel, peer1org1, Matchers.is(200), Matchers.is("100"), Matchers.anything());

        // Creating proposal for query
        final TransactionProposalRequest queryBProposalRequest = generateSACCQueryRequest(client, "b");
        Utils.sendTransactionProposalQuery(queryBProposalRequest, myChannel, org1peers, Matchers.is(200), Matchers.is("200"), Matchers.anything());
    }

    @Test
    public void testSBECCFirstNetwork() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUserOrg1TLS());

        Channel myChannel = Utils.getMyChannelFirstNetwork(client);

        List<Peer> org1peers = Utils.getPeersFromChannel(myChannel, "org1");
        List<Peer> org2peers = Utils.getPeersFromChannel(myChannel, "org2");
        List<Peer> peer0org2 = Utils.getPeersFromChannel(myChannel, "peer0.org2");

        System.out.println("Installing chaincode fabric-chaincode-example-sacc, packaged as gzip stream");
        InstallProposalRequest installProposalRequest = generateSBECCInstallRequest(client);
        Utils.sendInstallProposals(client, installProposalRequest,  org1peers);

        client.setUserContext(Utils.getAdminUserOrg2TLS());

        installProposalRequest = generateSBECCInstallRequest(client);
        Utils.sendInstallProposals(client, installProposalRequest,  org2peers);

        InstantiateProposalRequest instantiateProposal = generateSBECCInstantiateRequest(client);
        Utils.sendInstantiateProposal("sbecc", instantiateProposal, myChannel, peer0org2, myChannel.getOrderers());

        RunSBE(client, myChannel, "pub");
        RunSBE(client, myChannel, "priv");

    }

    void RunSBE(HFClient client, Channel channel,  String mode) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, ProposalException, InvalidArgumentException {
        List<Peer> peer0org1 = Utils.getPeersFromChannel(channel, "peer0.org1");
        List<Peer> peer0org2 = Utils.getPeersFromChannel(channel, "peer0.org2");
        List<Peer> allpeers0 = Utils.getPeersFromChannel(channel, "peer0");

        client.setUserContext(Utils.getUser1Org1TLS());
        TransactionProposalRequest proposal = generateSBECCTransactionRequest(client, "setval", mode, "foo");
        Utils.sendTransactionProposalInvoke(proposal, channel, peer0org1, channel.getOrderers());

        proposal = generateSBECCTransactionRequest(client, "getval", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org1, Matchers.is(200), Matchers.anything(), Matchers.is(ByteString.copyFrom("foo", StandardCharsets.UTF_8)));

        proposal = generateSBECCTransactionRequest(client, "addorgs", mode, "Org1MSP");
        Utils.sendTransactionProposalInvoke(proposal, channel, peer0org1, channel.getOrderers());

        proposal = generateSBECCTransactionRequest(client, "listorgs", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org1, Matchers.is(200), Matchers.anything(), Matchers.is(ByteString.copyFrom("[\"Org1MSP\"]", StandardCharsets.UTF_8)));

        proposal = generateSBECCTransactionRequest(client, "setval", mode, "val1");
        Utils.sendTransactionProposalInvoke(proposal, channel, peer0org1, channel.getOrderers());

        proposal = generateSBECCTransactionRequest(client, "getval", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org1, Matchers.is(200), Matchers.anything(), Matchers.is(ByteString.copyFrom("val1", StandardCharsets.UTF_8)));

        client.setUserContext(Utils.getUser1Org2TLS());
        proposal = generateSBECCTransactionRequest(client, "setval", mode, "val2");
        Utils.sendTransactionProposalInvoke(proposal, channel, peer0org2, channel.getOrderers(), true);

        proposal = generateSBECCTransactionRequest(client, "getval", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org2, Matchers.is(200), Matchers.anything(), Matchers.is(ByteString.copyFrom("val1", StandardCharsets.UTF_8)));

        client.setUserContext(Utils.getUser1Org1TLS());
        proposal = generateSBECCTransactionRequest(client, "addorgs", mode, "Org2MSP");
        Utils.sendTransactionProposalInvoke(proposal, channel, peer0org1, channel.getOrderers());

        proposal = generateSBECCTransactionRequest(client, "listorgs", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org1, Matchers.is(200), Matchers.anything(), Matchers.anyOf(Matchers.is(ByteString.copyFrom("[\"Org1MSP\",\"Org2MSP\"]", StandardCharsets.UTF_8)),Matchers.is(ByteString.copyFrom("[\"Org2MSP\",\"Org1MSP\"]", StandardCharsets.UTF_8))));

        client.setUserContext(Utils.getUser1Org2TLS());
        proposal = generateSBECCTransactionRequest(client, "setval", mode, "val3");
        Utils.sendTransactionProposalInvoke(proposal, channel, peer0org2, channel.getOrderers(), true);

        proposal = generateSBECCTransactionRequest(client, "getval", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org2, Matchers.is(200), Matchers.anything(), Matchers.is(ByteString.copyFrom("val1", StandardCharsets.UTF_8)));

        proposal = generateSBECCTransactionRequest(client, "setval", mode, "val4");
        Utils.sendTransactionProposalInvoke(proposal, channel, allpeers0, channel.getOrderers());

        client.setUserContext(Utils.getUser1Org1TLS());
        proposal = generateSBECCTransactionRequest(client, "getval", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org1, Matchers.is(200), Matchers.anything(), Matchers.is(ByteString.copyFrom("val4", StandardCharsets.UTF_8)));

        client.setUserContext(Utils.getUser1Org2TLS());
        proposal = generateSBECCTransactionRequest(client, "delorgs", mode, "Org1MSP");
        Utils.sendTransactionProposalInvoke(proposal, channel, peer0org2, channel.getOrderers(), true);

        proposal = generateSBECCTransactionRequest(client, "listorgs", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org2, Matchers.is(200), Matchers.anything(), Matchers.anyOf(Matchers.is(ByteString.copyFrom("[\"Org1MSP\",\"Org2MSP\"]", StandardCharsets.UTF_8)),Matchers.is(ByteString.copyFrom("[\"Org2MSP\",\"Org1MSP\"]", StandardCharsets.UTF_8))));

        proposal = generateSBECCTransactionRequest(client, "delorgs", mode, "Org1MSP");
        Utils.sendTransactionProposalInvoke(proposal, channel, allpeers0, channel.getOrderers());

        proposal = generateSBECCTransactionRequest(client, "listorgs", mode);
        Utils.sendTransactionProposalQuery(proposal, channel, peer0org2, Matchers.is(200), Matchers.anything(), Matchers.is(ByteString.copyFrom("[\"Org2MSP\"]", StandardCharsets.UTF_8)));

    }

    static public InstallProposalRequest generateNoBuildInstallRequest(HFClient client, String name, boolean useSrcPrefix) throws IOException, InvalidArgumentException {
        return Utils.generateInstallRequest(client, name, "1.0", "src/test/resources/NoBuildCC", useSrcPrefix);
    }

    static public InstallProposalRequest generateNoMainInstallRequest(HFClient client, String name, boolean useSrcPrefix) throws IOException, InvalidArgumentException {
        return Utils.generateInstallRequest(client, name, "1.0", "src/test/resources/NoMainCC", useSrcPrefix);
    }

    static public InstantiateProposalRequest generateInstantiateRequest(HFClient client, String name) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        return Utils.generateInstantiateRequest(client, name, "1.0", "src/test/resources/chaincodeendorsementpolicy.yaml", null, "init", "a", "100");
    }

    static public InstallProposalRequest generateSACCInstallRequest(HFClient client) throws IOException, InvalidArgumentException {
        return Utils.generateInstallRequest(client, "sacc", "1.0", "../fabric-chaincode-example-sacc", false);
    }

    static public TransactionProposalRequest generateSACCInvokeRequest(HFClient client, String key, String value) {
        return Utils.generateTransactionRequest(client, "sacc", "1.0", "set", key, value);
    }

    static public TransactionProposalRequest generateSACCQueryRequest(HFClient client, String key) {
        return Utils.generateTransactionRequest(client, "sacc", "1.0", "get", key);
    }

    private InstallProposalRequest generateSBECCInstallRequest(HFClient client) throws IOException, InvalidArgumentException {
        return Utils.generateInstallRequest(client, "sbecc", "1.0", "../fabric-chaincode-example-sbe");
    }

    static public InstantiateProposalRequest generateSBECCInstantiateRequest(HFClient client) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        return Utils.generateInstantiateRequest(client, "sbecc", "1.0", "src/test/resources/chaincodeendorsementpolicy_2orgs.yaml", "src/test/resources/collection_config.yaml", "init", new String[]{});
    }

    static public TransactionProposalRequest generateSBECCTransactionRequest(HFClient client, String func, String... args) {
        return Utils.generateTransactionRequest(client, "sbecc", "1.0", func, args);
    }
}
