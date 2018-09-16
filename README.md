# Hyperledger Fabric Shim for Java chaincode

This is the project for the HYperledger FAbric shim APIs for Java chaincode development. 
The following instructions are intended for a contributor or an early adopter and
describe the steps to build and test the library.

As an application developer, to learn about how to implement
**Smart Contracts** (also known as chaincode) for Hyperledger Fabric using
Java, visit the tutorial on `Chaincode for developers <https://hyperledger-fabric.readthedocs.io/en/latest/chaincode4ade.html>`__.

This project creates `fabric-chaincode-protos` and `fabric-chaincode-shim` jar
files for developers consumption and `hyperledger/fabric-javaenv` docker image
to run Java chaincode.

## Folder structure

The "fabric-chaincode-protos" folder contains protobuf definition files used by
Java shim to communicate with Fabric peer.

The "fabric-chaincode-shim" folder contains java shim classes that define Java
chaincode API and way to communicate with Fabric peer.

The "fabric-chaincode-docker" folder contains instructions for
`hyperledger/fabric-javaenv` docker image build.

The "fabric-chaincode-example-gradle" contains an example java chaincode gradle
project that includes sample chaincode and basic gradle build instructions.

The "fabric-chaincode-docs" contains Java shim documentation and examples.

## Prerequisites
* Java 8
* gradle 4.4

## Build shim

Clone the fabric shim for java chaincode repo.

```
git clone ssh://<gerrit id>@gerrit.hyperledger.org:29418/fabric-chaincode-java
```

Build and install java shim jars (proto and shim jars).
```
gradle clean build install
```

Build javaenv docker image, to have it locally.
```
gradle buildImage
```
