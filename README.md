# Hyperledger Fabric Java Chaincode support

[![Build Status](https://img.shields.io/travis/hyperledger/fabric-chaincode-java/master.svg)](https://travis-ci.org/hyperledger/fabric-chaincode-java)

This is the project provides support (shim) layer for development of Java chaincode for Fabric. The following instructions are oriented to a contributor or an early adopter and describes the steps to build and test the library.

This project publishes  `hyperledger/fabric-javaenv` docker image and `fabric-chaincode-shim` jar file for developers consumption.

As an application developer, to learn about how to implement "Smart Contracts" for the Hyperledger Fabric using java, please visit the TBD.

## Folder structure

`fabric-chaincode-protos` module contains protobuf files. Those files describe communication API between shim and fabric
and shared between fabric and shim. During build java source generated from `.proto` files.


`fabric-chaincode-shim` module contains actual shim code. The result jar file created by building this module.
 
 


