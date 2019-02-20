#!/bin/bash
./gradlew :fabric-chaincode-protos:build
./gradlew :fabric-chaincode-shim:build
./gradlew :fabric-chaincode-docker:copyAllDeps
cd fabric-chaincode-docker
docker build -t hyperledger/fabric-javaenv:latest .
docker tag hyperledger/fabric-javaenv:latest hyperledger/fabric-javaenv:ppc64le-latest
docker tag hyperledger/fabric-javaenv:latest hyperledger/fabric-javaenv:ppc64le-2.0.0
docker tag hyperledger/fabric-javaenv:latest hyperledger/fabric-javaenv:ppc64le-1.4.0
