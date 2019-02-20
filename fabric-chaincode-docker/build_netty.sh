#!/bin/bash

BINTARGETS="ppc64le s390x"
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
	git checkout netty-tcnative-parent-2.0.7.Final
	git apply $HOME/ppc.patch
	mvn clean install -pl \!openssl-dynamic -DskipTests
else
	exit 0
fi

