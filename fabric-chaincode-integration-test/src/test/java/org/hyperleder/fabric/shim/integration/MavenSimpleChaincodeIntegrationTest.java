package org.hyperleder.fabric.shim.integration;

import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.stream.Collectors;

public class MavenSimpleChaincodeIntegrationTest {

    private static final String CC_NAME = "SimpleChaincode";
    private static final String CC_VERSION = "1.0";

    @ClassRule
    public static DockerComposeContainer env = new DockerComposeContainer(
            new File("src/test/resources/first-network/docker-compose-cli.yaml")
    )
            .withLocalCompose(false)
            .withPull(true);

    @BeforeClass
    public static void setUp() throws Exception {
        Utils.setUp();
    }

    @Test
    public void testSimpelChaincodeFirstNetwork() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUserOrg1TLS());

        Channel myChannel = Utils.getMyChannelFirstNetwork(client);

        System.out.println("Installing chaincode SimpleChaincode, packaged as gzip stream");
        InstallProposalRequest installProposalRequest = generateSimpleChaincodeInstallRequest(client);
        Utils.sendInstallProposals(client, installProposalRequest, myChannel.getPeers().stream().filter(peer -> peer.getName().indexOf("org1") != -1).collect(Collectors.toList()));

        client.setUserContext(Utils.getAdminUserOrg2TLS());
        installProposalRequest = generateSimpleChaincodeInstallRequest(client);
        Utils.sendInstallProposals(client, installProposalRequest, myChannel.getPeers().stream().filter(peer -> peer.getName().indexOf("org2") != -1).collect(Collectors.toList()));

        InstantiateProposalRequest instantiateProposal = generateSimpleChaincodeInstantiateRequest(client);
        Utils.sendInstantiateProposal(CC_NAME, instantiateProposal, myChannel, myChannel.getPeers().stream().filter(peer -> peer.getName().indexOf("peer0.org2") != -1).collect(Collectors.toList()), myChannel.getOrderers());

    }

    private InstallProposalRequest generateSimpleChaincodeInstallRequest(HFClient client) throws IOException, InvalidArgumentException {
        return Utils.generateInstallRequest(client, CC_NAME, CC_VERSION, "../fabric-chaincode-example-maven");
    }

    static public InstantiateProposalRequest generateSimpleChaincodeInstantiateRequest(HFClient client) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        return Utils.generateInstantiateRequest(client, CC_NAME, CC_VERSION, "src/test/resources/chaincodeendorsementpolicy_2orgs.yaml", "src/test/resources/collection_config.yaml", "init", "a", "1", "b", "2");
    }

    static public TransactionProposalRequest generateSimpleChaincodeTransactionRequest(HFClient client, String func, String... args) {
        return Utils.generateTransactionRequest(client, CC_NAME, CC_VERSION, func, args);
    }
}
