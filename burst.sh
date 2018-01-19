#!/bin/bash
#made by BurningPixels

export FIREBIRD=$(dirname $0)/lib/firebird/$(getconf LONG_BIT)

MY_MAVEN_VERSION=3.5.0

MY_SELF=$0
MY_CMD=$1
MY_ARG=$2

function usage() {
    cat << EOF
usage: $0 [command] [arguments]

  load       [filename or url]  quick import by loading a binary dump
  loadsilent [filename or url]  quick import by loading a binary dump without interaction
  dump       [filename]         create a binary dump usable for doing a quick import
  help                          shows the help you just read
  compile                       compile jar and create docs using maven
EOF
}

function maybe_load_dump_usage () {
    if [ -z "$MY_ARG" ]; then
        usage
        exit 1
    fi
}

if [ -z `which java 2>/dev/null` ]; then
    echo please install java from eg. https://java.com/de/download/
    exit 1
fi

if [[ $# -gt 0 ]] ; then
    case "$MY_CMD" in
        "load")
            maybe_load_dump_usage
            java -cp burst.jar:conf brs.db.quicksync.LoadBinDump "$MY_ARG"
            ;;
        "loadsilent")
            maybe_load_dump_usage
            java -cp burst.jar:conf brs.db.quicksync.LoadBinDump "$MY_ARG" -y
            ;;
        "dump")
            maybe_load_dump_usage
            java -cp burst.jar:conf brs.db.quicksync.CreateBinDump "$MY_ARG"
            ;;
        "compile")
            if [ -d "maven/apache-maven-${MY_MAVEN_VERSION}" ]; then
                PATH=maven/apache-maven-${MY_MAVEN_VERSION}/bin:$PATH
            fi

            ## check if command exists
            if hash mvn 2>/dev/null; then
                mvn package
                mvn javadoc:javadoc-no-fork
                rm -rf html/ui/doc
                mkdir -p html/ui/doc
                cp -r target/site/apidocs/* html/ui/doc
                cp dist/tmp/burst.jar .
                echo a .zip file has been built for distribution in dist/, its contents are in dist/tmp
                echo Nevertheless, now you can start the wallet with ./burst.sh
            else
                echo This build method is no longer supported. Please install maven.
                echo https://maven.apache.org/install.html
                if hash wget 2>/dev/null; then
                    read -p "Do you want me to install a local copy of maven in this directory? " -n 1 -r
                    echo
                    if [[ $REPLY =~ ^[Yy]$ ]]; then
                        mkdir -p maven
                        cd maven
                        ## This is an official mirror
                        wget "http://mirror.23media.de/apache/maven/maven-3/$MY_MAVEN_VERSION/binaries/apache-maven-${MY_MAVEN_VERSION}-bin.tar.gz"
                        tar -xvzf "apache-maven-${MY_MAVEN_VERSION}-bin.tar.gz"
                        rm "apache-maven-$MY_MAVEN_VERSION-bin.tar.gz"
                        echo Please try again, it should work now. You might want to check if the environment variable JAVA_HOME points to a valid JDK.
                    fi
                fi
            fi
            ;;
        *)
            usage
            ;;
    esac
else
    java -cp burst.jar:conf brs.Burst
fi
