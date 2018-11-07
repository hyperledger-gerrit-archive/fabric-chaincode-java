package org.hyperleder.fabric.shim.integration;

import com.github.dockerjava.api.model.Container;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hamcrest.Matchers;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;


public class SACCIntegrationTest {
    @ClassRule
    public static DockerComposeContainer env = new DockerComposeContainer(
            new File("src/test/resources/basic-network/docker-compose.yml")
    )
            .withLocalCompose(false)
            .withPull(false);

    @BeforeClass
    public static void setUp() throws Exception {
        try {
            Utils.runWithTimeout(new Thread(() -> {
                try {
                    waitForCliContainerExecution();
                } catch (InterruptedException e) {

                }
                return;
            }
            ), 60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Got timeout, while waiting for cli execution");
        }

        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void TestSACCChaincodeInstallInstantiate() throws Exception{

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(getAdminUser());

        Channel myChannel = getMyChannel(client);

        System.out.println("Installing chaincode fabric-chaincode-example-sacc, packaged as gzip stream");
        InstallProposalRequest installProposalRequest = generateSACCInstallRequest(client);
        Collection<ProposalResponse> installResponces = client.sendInstallProposal(installProposalRequest, myChannel.getPeers());

        for (ProposalResponse response : installResponces) {
            if (response.getStatus() != ProposalResponse.Status.SUCCESS) {
                System.out.println("We have a problem, chaicode not installed: " + response.getMessage());
                fail ("We have a problem, chaicode not installed: " + response.getMessage());
            }
        }

        // Instantiating chaincode
        System.out.println("Instantiating chaincode: {a, 100}");
        InstantiateProposalRequest instantiateProposalRequest = generateSACCInstantiateRequest(client);

        // Sending proposal
        System.out.println("Sending instantiate proposal");
        Collection<ProposalResponse> instantiationResponces = myChannel.sendInstantiationProposal(instantiateProposalRequest, myChannel.getPeers());
        if (instantiationResponces == null || instantiationResponces.isEmpty()) {
            System.out.println("We have a problem, no responses to instantiate request");
            fail("We have a problem, no responses to instantiate request");
        }
        for (ProposalResponse response : instantiationResponces) {
            if (response.getStatus() != ProposalResponse.Status.SUCCESS) {
                System.out.println("We have a problem, chaicode not instantiated: " + response.getMessage());
                fail("We have a problem, chaicode not instantiated: " + response.getMessage());
            }
        }

        // Sending result transaction to orderers
        System.out.println("Sending instantiate transaction to orderers");

        Channel.NOfEvents nofEvents = Channel.NOfEvents.createNofEvents();
        if (!myChannel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)).isEmpty()) {
            nofEvents.addPeers(myChannel.getPeers(EnumSet.of(Peer.PeerRole.EVENT_SOURCE)));
        }


        CompletableFuture<BlockEvent.TransactionEvent> instantiateFuture = myChannel.sendTransaction(instantiationResponces,
                Channel.TransactionOptions.createTransactionOptions()
                        .orderers(myChannel.getOrderers())
                        .shuffleOrders(false)
                        .nOfEvents(nofEvents));
        try {
            instantiateFuture.get(120000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            System.out.println("We have problem waiting for transaction");
            fail("We have problem waiting for transaction send to orderers");
        }

        myChannel.getPeers().forEach(peer -> {
            try {
                assertThat("Peer " + peer.getName() + " doesn't have chaincode javacc installed and instantiated", myChannel.queryInstantiatedChaincodes(peer).stream().map(ccInfo -> ccInfo.getName()).collect(Collectors.toList()), Matchers.contains("javacc"));
            } catch (Exception e) {
                fail("Accessing instantiate chaincodes on peer " + peer.getName() + " resulted in exception " + e);
            }
        });
    }

    static public void waitForCliContainerExecution() throws InterruptedException{
        for (int i = 0; i < 20; i++) {
            AtomicBoolean foundCliContainer = new AtomicBoolean(false);
            List<Container> containers = DockerClientFactory.instance().client().listContainersCmd().withShowAll(true).exec();
            containers.forEach(container -> {
                for (String name : container.getNames()) {
                    if (name.indexOf("cli") != -1) {
                         if (container.getStatus().indexOf("Exited (0)") != -1) {
                             foundCliContainer.getAndSet(true);
                             break;
                         }
                    }
                }
            });
            if (foundCliContainer.get()) {
                return;
            }
            TimeUnit.SECONDS.sleep(10);
        }
    }

    static public User getAdminUser() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        // Loading admin user
        System.out.println("Loading org1 admin from disk");

        File userPrivateKeyFile = new File("src/test/resources/basic-network/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/276795ccedceb1d7923668307dcd9e124289c98b6e0a9731e71ba8d2193a7cce_sk");
        File userCertificateFile =  new File("src/test/resources/basic-network/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem");
        return Utils.getUser("peeradmin", "Org1MSP", userPrivateKeyFile, userCertificateFile);
    }

    static public Channel getMyChannel(HFClient client) throws InvalidArgumentException, TransactionException {
        // Accessing channel, should already exist
        System.out.println("Accessing channel");
        Channel myChannel = client.newChannel("mychannel");

        System.out.println("Setting channel configuration");
        final List<Peer> peers = new LinkedList<>();
        peers.add(client.newPeer("peer0.org1.example.com", "grpc://localhost:7051"));

        final List<Orderer> orderers = new LinkedList<>();
        orderers.add(client.newOrderer("orderer.example.com", "grpc://localhost:7050"));

        myChannel.addEventHub(client.newEventHub("peer0.org1.example.com",  "grpc://localhost:7053"));

        for (Orderer orderer : orderers) {
            myChannel.addOrderer(orderer);
        }

        for (Peer peer : peers) {
            myChannel.addPeer(peer);
        }
        myChannel.initialize();

        return myChannel;
    }

    static public InstallProposalRequest generateSACCInstallRequest(HFClient client) throws IOException, InvalidArgumentException {
        final InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        final ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("javacc")
                .setVersion("1.0")
                .build();


        final String chaincodeLocation = "../fabric-chaincode-example-sacc";
        installProposalRequest.setChaincodeID(chaincodeID);
        installProposalRequest.setChaincodeLanguage(TransactionRequest.Type.JAVA);
        installProposalRequest.setChaincodeInputStream(Utils.generateTarGzInputStream(new File(chaincodeLocation), "src"));
        installProposalRequest.setChaincodeVersion("1.0");

        return installProposalRequest;
    }

    static public InstantiateProposalRequest generateSACCInstantiateRequest(HFClient client) throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException {
        // Instantiating chaincode
        System.out.println("Instantiating chaincode: {}");
        final ChaincodeID chaincodeID = ChaincodeID.newBuilder()
                .setName("javacc")
                .setVersion("1.0")
                .build();

        // Building proposal
        System.out.println("Building instantiate proposal");
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(120000);
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setChaincodeLanguage(TransactionRequest.Type.JAVA);
        instantiateProposalRequest.setArgs(new String[] {"a", "100"});
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);

        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File("src/test/resources/chaincodeendorsementpolicy.yaml"));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        return instantiateProposalRequest;

    }
}
