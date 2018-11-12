package org.hyperleder.fabric.shim.integration;

import com.github.dockerjava.api.model.Container;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.testcontainers.DockerClientFactory;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Utils {

    static public User getUser(String name, String mspId, File privateKeyFile, File certificateFile)
            throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {

        try {

            final String certificate = new String(IOUtils.toByteArray(new FileInputStream(certificateFile)), "UTF-8");

            final PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privateKeyFile)));

            User user = new User() {

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public Set<String> getRoles() {
                    return null;
                }

                @Override
                public String getAccount() {
                    return null;
                }

                @Override
                public String getAffiliation() {
                    return null;
                }

                @Override
                public Enrollment getEnrollment() {
                    return new Enrollment() {

                        @Override
                        public PrivateKey getKey() {
                            return privateKey;
                        }

                        @Override
                        public String getCert() {
                            return certificate;
                        }
                    };
                }

                @Override
                public String getMspId() {
                    return mspId;
                }

            };

            return user;
        } catch (IOException e) {
            e.printStackTrace();
            throw e;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw e;
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
            throw e;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            throw e;
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw e;
        }

    }

    static PrivateKey getPrivateKeyFromBytes(byte[] data)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException {
        final Reader pemReader = new StringReader(new String(data));

        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getPrivateKey(pemPair);

        return privateKey;
    }

    public static InputStream generateTarGzInputStream(File src, String pathPrefix) throws IOException {
        File sourceDirectory = src;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(500000);

        String sourcePath = sourceDirectory.getAbsolutePath();

        TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(new BufferedOutputStream(bos)));
        archiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        try {
            Collection<File> childrenFiles = org.apache.commons.io.FileUtils.listFiles(sourceDirectory, null, true);

            ArchiveEntry archiveEntry;
            FileInputStream fileInputStream;
            for (File childFile : childrenFiles) {
                String childPath = childFile.getAbsolutePath();
                String relativePath = childPath.substring((sourcePath.length() + 1), childPath.length());

                if (pathPrefix != null) {
                    relativePath = org.hyperledger.fabric.sdk.helper.Utils.combinePaths(pathPrefix, relativePath);
                }

                relativePath = FilenameUtils.separatorsToUnix(relativePath);

                archiveEntry = new TarArchiveEntry(childFile, relativePath);
                fileInputStream = new FileInputStream(childFile);
                archiveOutputStream.putArchiveEntry(archiveEntry);

                try {
                    IOUtils.copy(fileInputStream, archiveOutputStream);
                } finally {
                    IOUtils.closeQuietly(fileInputStream);
                    archiveOutputStream.closeArchiveEntry();
                }
            }
        } finally {
            IOUtils.closeQuietly(archiveOutputStream);
        }

        return new ByteArrayInputStream(bos.toByteArray());
    }

    public static void runWithTimeout(Runnable callable, long timeout, TimeUnit timeUnit) throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                callable.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            executor.execute(t);
            if (!latch.await(timeout, timeUnit)) {
                throw new TimeoutException();
            }
        } finally {
            executor.shutdown();
            t.interrupt();
        }
    }

    static public void waitForCliContainerExecution() throws InterruptedException {
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
        File userCertificateFile = new File("src/test/resources/basic-network/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts/Admin@org1.example.com-cert.pem");
        return getUser("peeradmin", "Org1MSP", userPrivateKeyFile, userCertificateFile);
    }

    static public User getUser1() throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        // Loading admin user
        System.out.println("Loading org1 admin from disk");

        File userPrivateKeyFile = new File("src/test/resources/basic-network/crypto-config/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/keystore/0cd56151db5d102e209b295f16b562dd2fba7a41988341cd4a783a9f0520855f_sk");
        File userCertificateFile = new File("src/test/resources/basic-network/crypto-config/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/signcerts/User1@org1.example.com-cert.pem");
        return getUser("peeruser1", "Org1MSP", userPrivateKeyFile, userCertificateFile);
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

        myChannel.addEventHub(client.newEventHub("peer0.org1.example.com", "grpc://localhost:7053"));

        for (Orderer orderer : orderers) {
            myChannel.addOrderer(orderer);
        }

        for (Peer peer : peers) {
            myChannel.addPeer(peer);
        }
        myChannel.initialize();

        return myChannel;
    }
}
