## Hyperledger Fabric Shim for Java chaincodes

This is the project for the fabric shim for java chaincodes development. The following instructions are oriented to a contributor or an early adopter and describes the steps to build and test the library.

As an application developer, to learn about how to implement **"Smart Contracts"** for the Hyperledger Fabric using java, Please visit the [documentation].

This project creates `fabric-chaincode-protos` and `fabric-chaincode-shim` jar files for developers consumption and `hyperledger/fabric-javaenv` docker image to run java chaincode.

### Folder structure

The "fabric-chaincode-protos" folder contains protobuf definition files used by java shim to communicate with fabric peer

The "fabric-chaincode-shim" folder contains java shim classes that define java chaincode API and way to communicate with fabric peer

The "fabric-chaincode-docker" folder contains instructions for `hyperledger/fabric-javaenv` docker image build

The "fabric-chaincode-example-gradle" contains example java chaincode gradle project that includes sample chaincode and basic gradle build instructions

The "fabric-chaincode-docs" contains java shim documentation and examples

### Pre-requisites
* Java 8
* gradle 4.4

### Build shim

Clone the fabric shim for java chaincode repo

```
git clone ssh://<gerrit id>@gerrit.hyperledger.org:29418/fabric-chaincode-java
```

Build and install java shim jars (proto and shim jars)
```
gradle clean build install
```

Build javaenv docker image, to have it locally
```
gradle buildImage
```

