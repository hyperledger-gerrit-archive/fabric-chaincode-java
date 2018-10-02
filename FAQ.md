# Frequently Asked Questions - Hyperledger Fabric Shim for Java chaincode

### Q. How to use latest master code?

Checkout and build latest code from master, including jars and docker images. Make your chanincode depend on
java shim master version and not on version from maven central.

### Q. How to build latest master code?

#### Install prerequisites

Make sure you installed all [fabric prerequisites](https://hyperledger-fabric.readthedocs.io/en/latest/prereqs.html)

Install java shim specific prerequisites:
* Java 8
* gradle 4.4

#### Build shim

Clone the fabric shim for java chaincode repo.

```
git clone https://github.com/hyperledger/fabric-chaincode-java.git
```

Build java shim jars (proto and shim jars) and install them to local maven repository.
```
cd fabric-chaincode-java
gradle clean build install
```

Build javaenv docker image, to have it locally.
```
gradle buildImage
```

### Q. Can I use different version of java shim and fabric core?

Yes, you can. But only if no API changes were introduced.

For example, you want to use java shim version 1.4.0-SNAPSHOT and fabric 1.3.0 and no single API in peer/chaincode communication was changed.

Go to fabric each peer `core.yaml` and update `chaincode.java.runtime` to `$(DOCKER_NS)/fabric-javaenv:$(ARCH)-1.4.0-SNAPSHOT`

### Q. Can I install chaincode depend on version X of shim on top of fabric version Y?

It depends.

* If any API change in shim was introduced between version - no, you can't.
* If no API changes and both X and Y are stable version - yes, without any reconfiguration.
* If X is snapshot version, some reconfiguration needed
    * Build shim locally, including docker image
    * On each computer you plan to run fabric peer, make sure fabric-javaenv docker image exist
    * On each java peer update `core.yaml` to reference to correct fabric-javaenv docker image