package org.hyperleder.fabric.shim.integration;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
import java.security.Security;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class KotlinIntegrationTest {
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
                    Utils.waitForCliContainerExecution();
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
    public void TestKotlinExampleChaincodeInstallInstantiateInvokeQuery() throws Exception {

        final CryptoSuite crypto = CryptoSuite.Factory.getCryptoSuite();

        // Create client and set default crypto suite
        System.out.println("Creating client");
        final HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(crypto);

        client.setUserContext(Utils.getAdminUser());

        Channel myChannel = Utils.getMyChannel(client);

        System.out.println("Installing chaincode fabric-chaincode-example-sacc, packaged as gzip stream");
        InstallProposalRequest installProposalRequest = generateKotlinExampleInstallRequest(client);
        Collection<ProposalResponse> installResponces = client.sendInstallProposal(installProposalRequest, myChannel.getPeers());

        for (ProposalResponse response : installResponces) {
            if (response.getStatus() != ProposalResponse.Status.SUCCESS) {
                System.out.println("We have a problem, chaicode not installed: " + response.getMessage());
                fail("We have a problem, chaicode not installed: " + response.getMessage());
            }
        }

        // Instantiating chaincode
        InstantiateProposalRequest instantiateProposalRequest = generateKotlinExampleInstantiateRequest(client);

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
                assertThat("Peer " + peer.getName() + " doesn't have chaincode kotlincc installed and instantiated", myChannel.queryInstantiatedChaincodes(peer).stream().map(ccInfo -> ccInfo.getName()).collect(Collectors.toList()), Matchers.contains("kotlincc"));
            } catch (Exception e) {
                fail("Accessing instantiate chaincodes on peer " + peer.getName() + " resulted in exception " + e);
            }
        });

        client.setUserContext(Utils.getUser1());

        System.out.println("Creating proposal for set(b, 200)");
        final TransactionProposalRequest proposalRequest = generateKotlinExampleInvokeRequest(client, "Alice", "Bob", "10");

        // Send proposal and wait for responses
        System.out.println("Sending proposal for invokeset(b, 200)");
        final Collection<ProposalResponse> responses = myChannel.sendTransactionProposal(proposalRequest, myChannel.getPeers());

        // Sending transaction to orderers
        System.out.println("Sending transaction for invokeset(b, 200)");

        CompletableFuture<BlockEvent.TransactionEvent> txFuture = myChannel.sendTransaction(responses,
                Channel.TransactionOptions.createTransactionOptions()
                        .orderers(myChannel.getOrderers())
                        .shuffleOrders(false)
                        .nOfEvents(nofEvents));

        BlockEvent.TransactionEvent event = null;
        try {
            event = txFuture.get(50000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            System.out.println("Exception " + e + " during wait");
            fail("Exception " + e + " during wait");
        }

        if (event == null) {
            System.out.println("Something wrong, event is null");
            fail("Something wrong, event is null");
        }

        System.out.println("Result transaction event");

        event.getTransactionActionInfos().forEach(i -> {
            ChaincodeEvent e = i.getEvent();
            System.out.println(i);
            if (e != null) {
                System.out.println(e);
            }
        });

        // Creating proposal for query
        final TransactionProposalRequest queryAProposalRequest = generateKotlinExampleQueryRequest(client, "Alice");

        // Send proposal and wait for responses
        System.out.println("Sending proposal for get(a)");
        final Collection<ProposalResponse> queryAResponses = myChannel.sendTransactionProposal(queryAProposalRequest, myChannel.getPeers());

        for (ProposalResponse resp : queryAResponses) {
            System.out.println("Responce from peer " + resp.getPeer().getName() + " is \n" + resp.getProposalResponse().getResponse() + "\n" + resp.getProposalResponse().getResponse().getPayload().toStringUtf8());
            assertThat(resp.getProposalResponse().getResponse().getStatus(), Matchers.is(200));
            assertThat(resp.getProposalResponse().getResponse().getMessage(), Matchers.is("90"));
        }

        // Creating proposal for query
        final TransactionProposalRequest queryBProposalRequest = generateKotlinExampleQueryRequest(client, "Bob");
        // Send proposal and wait for responses
        System.out.println("Sending proposal for get(b)");
        final Collection<ProposalResponse> queryBResponses = myChannel.sendTransactionProposal(queryBProposalRequest, myChannel.getPeers());

        for (ProposalResponse resp : queryBResponses) {
            System.out.println("Responce from peer " + resp.getPeer().getName() + " is \n" + resp.getProposalResponse().getResponse());
            assertThat(resp.getProposalResponse().getResponse().getStatus(), Matchers.is(200));
            assertThat(resp.getProposalResponse().getResponse().getMessage(), Matchers.is("210"));
        }


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
