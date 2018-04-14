FROM hyperledger/fabric-baseimage:amd64-0.4.8
MAINTAINER Gennady Laventman "gennady@il.ibm.com"
ADD build/distributions/ /root/
RUN mkdir -p /root/chaincode-java/chaincode
RUN chmod +x /root/chaincode-java/start
