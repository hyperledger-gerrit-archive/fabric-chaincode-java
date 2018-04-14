FROM hyperledger/fabric-baseimage:amd64-0.4.8
MAINTAINER Gennady Laventman "gennady@il.ibm.com"
RUN apt-get update
RUN apt-get install zip
RUN curl  -s "https://get.sdkman.io" | bash

# Install gradle and maven
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; sdk install gradle 4.6; sdk install maven 3.5.0"


ADD build/distributions/ /root/

RUN mkdir -p /root/chaincode-java/chaincode
RUN mkdir -p /root/chaincode-java/chaincode/src
RUN mkdir -p /root/chaincode-java/chaincode/build/out
RUN chmod +x /root/chaincode-java/start
RUN chmod +x /root/chaincode-java/build.sh

WORKDIR /root/chaincode-java/shim-src
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; gradle clean"
WORKDIR /root/chaincode-java/shim-src/fabric-chaincode-protos
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; gradle clean build install publishToMavenLocal"
WORKDIR /root/chaincode-java/shim-src/fabric-chaincode-protos/build/publications/protosJar/
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; mvn -f pom-default.xml compile"
WORKDIR /root/chaincode-java/shim-src/fabric-chaincode-shim
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; gradle clean build install publishToMavenLocal"
WORKDIR /root/chaincode-java/shim-src/fabric-chaincode-shim/build/publications/shimJar/
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; mvn -f pom-default.xml compile"
WORKDIR /root/chaincode-java/example-src/fabric-chaincode-example-gradle
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; gradle build shadowJar"
WORKDIR /root/chaincode-java/example-src/fabric-chaincode-example-maven
RUN /bin/bash -c "source /root/.sdkman/bin/sdkman-init.sh; mvn compile package"
WORKDIR /root/chaincode-java
RUN rm -rf example-src/*
RUN rm -rf shim-src

