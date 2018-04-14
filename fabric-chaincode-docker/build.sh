#!/bin/bash

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="build.sh"
APP_BASE_NAME=`basename "$0"`

if [-d "${APP_HOME}/chaincode/build/out"]
then
    rm -rf ${APP_HOME}/chaincode/build/out/*
else
    mkdir -p ${APP_HOME}/chaincode/build/out
fi

if [-f "${APP_HOME}/src/build.gradle"]
then
    buildGradle ${APP_HOME}/chaincode/src/
else
    buildMaven ${APP_HOME}/chaincode/src/
fi

buildGradle() {
    cd "$1" > /dev/null
    echo "Gradle build"
    gradle build shadowJar
    cp build/libs/chaincode.jar  ${APP_HOME}/chaincode/build/out/
    cd "$SAVED" >/dev/null
}

buildMaven() {
    cd "$1" > /dev/null
    echo "Maven build"
    mvn compile package
    cp target/chaincode.jar  ${APP_HOME}/chaincode/build/out/
    cd "$SAVED" >/dev/null
}