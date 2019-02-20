#!/bin/bash
./gradlew :fabric-chaincode-protos:build
./gradlew :fabric-chaincode-shim:build
./gradlew :fabric-chaincode-docker:copyAllDeps
cd fabric-chaincode-docker
docker build -t hyperledger/fabric-javaenv:latest .
docker tag hyperledger/fabric-javaenv:latest hyperledger/fabric-javaenv:s390x-latest
docker tag hyperledger/fabric-javaenv:latest hyperledger/fabric-javaenv:s390x-2.0.0
docker tag hyperledger/fabric-javaenv:latest hyperledger/fabric-javaenv:s390x-1.4.0
