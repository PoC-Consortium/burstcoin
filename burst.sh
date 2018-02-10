#!/bin/bash

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
  h2shell                       open a H2 shell for DB manipulation
  help                          shows the help you just read
  compile                       compile jar and create docs using maven
  upgrade                       upgrade the config files to BRS format
EOF
}

function maybe_load_dump_usage () {
    if [ -z "$MY_ARG" ]; then
        usage
        exit 1
    fi
}

function upgrade_conf () {
    BRS_CFG_NAME="conf/nxt-default.properties"

    if [ -r $BRS_CFG_NAME ]
    then
        BRS=$(<$BRS_CFG_NAME)    # read in the config file content
        # P2P-related params
        BRS="${BRS//nxt\.shareMyAddress/P2P.shareMyAddress}"
        BRS="${BRS//nxt\.peerServerPort/P2P.Port}"
        BRS="${BRS//nxt\.peerServerHost/P2P.Listen}"
        BRS="${BRS//nxt\.myAddress/P2P.myAddress}"
        BRS="${BRS//nxt\.myPlatform/P2P.myPlatform}"
        BRS="${BRS//nxt\.wellKnownPeers/P2P.BootstrapPeers}"
        BRS="${BRS//burst\.rebroadcastPeers/P2P.rebroadcastTo}"
        BRS="${BRS//burst\.connectWellKnownFirst/P2P.NumBootstrapConnections}"
        BRS="${BRS//nxt\.knownBlacklistedPeers/P2P.BlacklistedPeers}"
        BRS="${BRS//brs\.maxNumberOfConnectedPublicPeers/P2P.MaxConnections}"
        BRS="${BRS//nxt\.connectTimeout/P2P.TimeoutConnect_ms}"
        BRS="${BRS//nxt\.readTimeout/P2P.TimeoutRead_ms}"
        BRS="${BRS//nxt\.peerServerIdleTimeout/P2P.TimeoutIdle_ms}"
        BRS="${BRS//nxt\.blacklistingPeriod/P2P.BlacklistingTime_ms}"

        # P2P Hallmarks
        BRS="${BRS//nxt\.enableHallmarkProtection/P2P.HallmarkProtection}"
        BRS="${BRS//nxt\.myHallmark/P2P.myHallmark}"
        BRS="${BRS//nxt\.pushThreshold/P2P.HallmarkPush}"
        BRS="${BRS//nxt\.pullThreshold/P2P.HallmarkPull}"
        BRS="${BRS///}"
        BRS="${BRS///}"
        BRS="${BRS///}"

        # JETTY pass-through params
        BRS="${BRS//nxt\.enablePeerServerDoSFilter/JETTY.DoSFilter}"
        BRS="${BRS//nxt\.peerServerDoSFilter.maxRequestsPerSec/JETTY.DoSFilter.maxRequestsPerSec}"
        BRS="${BRS//nxt\.peerServerDoSFilter.delayMs/JETTY.DoSFilter.delayMs}"
        BRS="${BRS//nxt\.peerServerDoSFilter.maxRequestMs/JETTY.DoSFilter.maxRequestMs}"

        # TEST-related params (TestNet)
        BRS="${BRS//nxt\.isTestnet/TEST.Net}"
        BRS="${BRS//nxt\.testnetPeers/TEST.Peers}"

        # API-related params
        BRS="${BRS//nxt.enableAPIServer/}"
        BRS="${BRS///}"
        BRS="${BRS///}"
        BRS="${BRS///}"
        BRS="${BRS///}"
        
        # DB-related params
        # GPU-related params
        BRS="${BRS//burst\.oclVerify/GPU.Acceleration}"
        BRS="${BRS//burst\.oclAuto/GPU.AutoDetect}"
        BRS="${BRS//burst\.oclPlatform/GPU.PlatformIdx}"
        BRS="${BRS//burst\.oclDevice/GPU.DeviceIdx}"
        BRS="${BRS//burst\.oclMemPercent/GPU.MemPercent}"
        BRS="${BRS//burst\.oclHashesPerEnqueue/GPU.HashesPerBatch}"
        
        BRS="${BRS///}"
        echo "$BRS" > conf/brs-default.properties.test
    else
        echo "$BRS_CFG_NAME not present or not readable."
        exit 1
    fi
}

if [ -z `which java 2>/dev/null` ]; then
    echo "please install java from eg. https://java.com/download/"
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
        "upgrade")
            upgrade_conf
            ;;
        "h2shell")
            java -cp burst.jar org.h2.tools.Shell
            ;;
        *)
            usage
            ;;
    esac
else
    java -cp burst.jar:conf brs.Burst
fi
