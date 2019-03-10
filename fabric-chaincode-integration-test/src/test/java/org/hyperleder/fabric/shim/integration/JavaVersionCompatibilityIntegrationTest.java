package org.hyperleder.fabric.shim.integration;

import com.github.dockerjava.api.exception.ConflictException;
import com.google.protobuf.ByteString;
import org.hamcrest.Matchers;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.shim.Chaincode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.stream.Collectors;

public class JavaVersionCompatibilityIntegrationTest {

    private static final String CC_NAME = "SimpleChaincode";
    private static final Path CHAINCODE_PATH = Paths.get("../fabric-chaincode-example-java-version-compatibility");

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

    @AfterClass
    public static void shutDown() throws Exception {

        try {
            Utils.removeDevContainerAndImages();
        } catch (ConflictException e) {
            //not relevant
        }

        removeFile("pom.xml");
        removeFile("build.gradle");

    }

    @Test
    public void testGradleJava_1_8_Compatibility() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        createAndRun(8, CC_NAME + "gradle" + 8, false);
    }

    @Test
    public void testGradleJava_1_9_Compatibility() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        createAndRun(9, CC_NAME + "gradle" + 9, false);
    }

    @Test
    public void testGradleJava1_10_Compatibility() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        createAndRun(10, CC_NAME + "gradle" + 10, false);
    }

    @Test
    public void testMvnJava1_8_Compatibility() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        createAndRun(8, CC_NAME + "mvn" + 8, true);
    }

    @Test
    public void testMvnJava1_9_Compatibility() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        createAndRun(9, CC_NAME + "mvn" + 9, true);
    }

    @Test
    public void testMvnJava1_10_Compatibility() throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        createAndRun(10, CC_NAME + "mvn" + 10, true);
    }

    private void createAndRun(int version, String ccName, boolean isMvn) throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, TransactionException, ProposalException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        System.out.println("Creating client");

        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);
        client.setUserContext(Utils.getAdminUserOrg1TLS());
        Channel myChannel = Utils.getMyChannelFirstNetwork(client);

        String type;
        String fileName;

        if (isMvn) {

            createPomFile(version);
            type = "MAVEN";
            fileName = "pom.xml";

        } else {

            createGradleFile(version);
            type = "GRADLE";
            fileName = "build.gradle";

        }


        run(client, myChannel, ccName, version, type);
        removeFile(fileName);

        myChannel.shutdown(true);
    }

    private void run(HFClient client, Channel myChannel, String ccName, int javaVersion, String type) throws ProposalException, InvalidArgumentException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, ChaincodeCollectionConfigurationException, ChaincodeEndorsementPolicyParseException {

        String version = "1." + javaVersion + "." + type;
        System.out.println("Installing chaincode SimpleChaincode, packaged as gzip stream");
        InstallProposalRequest installProposalRequest = generateSimpleChaincodeInstallRequest(client, ccName, version);
        Utils.sendInstallProposals(client, installProposalRequest, myChannel.getPeers().stream().filter(peer -> peer.getName().contains("org1")).collect(Collectors.toList()));

        client.setUserContext(Utils.getAdminUserOrg2TLS());
        installProposalRequest = generateSimpleChaincodeInstallRequest(client, ccName, version);
        Utils.sendInstallProposals(client, installProposalRequest, myChannel.getPeers().stream().filter(peer -> peer.getName().contains("org2")).collect(Collectors.toList()));

        InstantiateProposalRequest instantiateProposal = generateSimpleChaincodeInstantiateRequest(client, ccName, version);
        Utils.sendInstantiateProposal(ccName, instantiateProposal, myChannel, myChannel.getPeers().stream().filter(peer -> peer.getName().contains("peer0.org2")).collect(Collectors.toList()), myChannel.getOrderers());

        runTransfer(client, ccName, version, myChannel);

    }

    private void createGradleFile(int version) throws IOException {

        Path path = Paths.get("../fabric-chaincode-example-java-version-compatibility");
        String buildFild = Files.lines(path.resolve("build.gradle.tpl")).map(l -> l.replace("{{JAVA-VERSION}}", "1." + version)).collect(Collectors.joining("\n"));
        Files.write(path.resolve("build.gradle"), buildFild.getBytes(Charset.forName("UTF-8")));

    }

    private void createPomFile(int version) throws IOException {

        Path path = Paths.get("../fabric-chaincode-example-java-version-compatibility");
        String pom = Files.lines(path.resolve("pom.xml.tpl")).map(l -> l.replace("{{JAVA-VERSION}}", "1." + version)).collect(Collectors.joining("\n"));
        Files.write(path.resolve("pom.xml"), pom.getBytes(Charset.forName("UTF-8")));

    }

    private void runTransfer(HFClient client, String ccName, String version, Channel channel) throws
            NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException, IOException, ProposalException, InvalidArgumentException {
        client.setUserContext(Utils.getUser1Org1TLS());
        TransactionProposalRequest proposal = generateSimpleChaincodeInvokeRequest(client, ccName, version, "a", "b", "10");
        Utils.sendTransactionProposalInvoke(proposal, channel, channel.getPeers().stream().filter(peer -> peer.getName().contains("peer0.org1")).collect(Collectors.toList()), channel.getOrderers());

        executeAndValidateQueryOnAccount(client, channel, ccName, version, "a", "peer0.org1", "90");
        executeAndValidateQueryOnAccount(client, channel, ccName, version, "b", "peer0.org1", "210");
        executeAndValidateQueryOnAccount(client, channel, ccName, version, "a", "peer0.org2", "90");
        executeAndValidateQueryOnAccount(client, channel, ccName, version, "b", "peer0.org2", "210");
    }

    private void executeAndValidateQueryOnAccount(HFClient client, Channel channel, String ccName, String version, String keyAccount, String
            peerName, String expectedAmount) throws ProposalException, InvalidArgumentException {
        TransactionProposalRequest proposal = generateSimpleChaincodeQueryRequest(client, ccName, version, keyAccount);
        Utils.sendTransactionProposalQuery(
                proposal,
                channel,
                channel.getPeers()
                        .stream()
                        .filter(peer -> peer.getName().contains(peerName))
                        .collect(Collectors.toList()),
                Matchers.is(Chaincode.Response.Status.SUCCESS.getCode()),
                Matchers.anything(),
                Matchers.is(ByteString.copyFrom(expectedAmount, StandardCharsets.UTF_8))
        );
    }

    private InstallProposalRequest generateSimpleChaincodeInstallRequest(HFClient client, String ccName, String version) throws
            IOException, InvalidArgumentException {
        return Utils.generateInstallRequest(client, ccName, version, "../fabric-chaincode-example-java-version-compatibility");
    }

    private InstantiateProposalRequest generateSimpleChaincodeInstantiateRequest(HFClient client, String ccName, String version) throws
            InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ChaincodeCollectionConfigurationException {
        return Utils.generateInstantiateRequest(
                client,
                ccName,
                version,
                "src/test/resources/chaincodeendorsementpolicy_2orgs.yaml",
                "src/test/resources/collection_config.yaml",
                "init", "a", "100", "b", "200");
    }

    private TransactionProposalRequest generateSimpleChaincodeTransactionRequest(HFClient client, String ccName, String
            func, String version, String... args) {
        return Utils.generateTransactionRequest(client, ccName, version, func, args);
    }

    private TransactionProposalRequest generateSimpleChaincodeInvokeRequest(HFClient client, String ccName, String version, String
            from, String to, String amount) {
        return Utils.generateTransactionRequest(client, ccName, version, "invoke", from, to, amount);
    }

    private TransactionProposalRequest generateSimpleChaincodeQueryRequest(HFClient client, String ccName, String version, String key) {
        return Utils.generateTransactionRequest(client, ccName, version, "query", key);
    }

    private static void removeFile(String fileName) throws IOException {
        Path pomPath = CHAINCODE_PATH.resolve(fileName);
        if (Files.exists(pomPath)) {
            Files.delete(pomPath);
        }
    }
}
