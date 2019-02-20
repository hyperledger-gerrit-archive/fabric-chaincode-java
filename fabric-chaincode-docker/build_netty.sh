#!/bin/bash

BINTARGETS="ppc64le s390x"
ZSYSTEM_ARCH="s390x"
if echo $BINTARGETS | grep -q `uname -m`; then
	apt --fix-broken install -y
	apt autoremove -y
	apt-get install -y git make gcc g++ cmake ninja-build software-properties-common automake autoconf libtool build-essential libapreq2-dev perl libssl-dev
	export GOPATH="/opt/gopath"
	export GOROOT="/opt/go"
	mkdir -p $GOPATH
	ARCH=`uname -m | sed 's|i686|386|' | sed 's|x86_64|amd64|'`
	GO_VER=1.11.5
	cd /tmp
	wget --quiet --no-check-certificate https://storage.googleapis.com/golang/go${GO_VER}.linux-${ARCH}.tar.gz
	tar -xvf go${GO_VER}.linux-${ARCH}.tar.gz
	mv go $GOROOT
	chmod 775 $GOROOT
	export PATH=$GOROOT/bin:$GOPATH/bin:$PATH
	cd $HOME
	git clone https://github.com/netty/netty-tcnative.git
	cd netty-tcnative
	git checkout netty-tcnative-parent-2.0.15.Final
	if echo $ZSYSTEM_ARCH | grep -q `uname -m`; then
        mvn clean install -pl openssl-static -DskipTests
        cp /root/.m2/repository/io/netty/netty-tcnative-openssl-static/2.0.15.Final/netty-tcnative-openssl-static-2.0.15.Final-linux-ppcle_64.jar /root/.m2/repository/io/netty/netty-tcnative-openssl-static/2.0.15.Final/netty-tcnative-openssl-static-2.0.15.Final.jar
	else
        mvn clean install -pl \!openssl-dynamic -DskipTests
        cp /root/.m2/repository/io/netty/netty-tcnative-boringssl-static/2.0.15.Final/netty-tcnative-boringssl-static-2.0.15.Final-linux-ppcle_64.jar /root/.m2/repository/io/netty/netty-tcnative-boringssl-static/2.0.15.Final/netty-tcnative-boringssl-static-2.0.15.Final.jar
	fi
else
	exit 0
fi

