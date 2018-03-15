<img align="right" width="120" height="120" title="Burst Logo" src="https://raw.githubusercontent.com/PoC-Consortium/Marketing_Resources/master/BURST_LOGO/PNG/icon_blue.png" />

# Burstcoin Wallet

[![Get Support at https://discord.gg/NKXGM6N](https://img.shields.io/badge/join-discord-blue.svg)](https://discord.gg/NKXGM6N)
[![Build Status](https://api.travis-ci.org/PoC-Consortium/burstcoin.svg?branch=master)](https://travis-ci.org/PoC-Consortium/burstcoin?branch=master) 
![Quality Gate](https://sonarqube.com/api/badges/gate?key=burstcoin:burstcoin)
[![MIT](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE.txt)

The world's first HDD-mined cryptocurrency using an energy efficient
and fair Proof-of-Capacity (PoC) consensus algorithm.

This wallet version is developed and maintained by the PoC consortium
(PoCC) and supports a multitude of database backends. The two builtin
backends are:
- MariaDB (recommended, but complex installation)
- H2 (for compatibility/migration purposes)

Other DB backends are supported by the Burstcoin DB manager:
https://github.com/PoC-Consortium/burstcoin-db-manager


### Software Installation

#### Linux (Debian, Ubuntu)

Please take a look at http://package.cryptoguru.org/ where you can
find the burstcoincg package. This will take care of the MariaDB
installation for you.

#### Windows

###### MariaDb

In the conf directory, copy brs-default.properties into a new file named brs.properties.

Download and install MariaDB https://mariadb.com/downloads/mariadb-tx

The MariaDb installation will ask to setup a password for the root user. 
Add this password to the brs.properties file created above in the following section:
```
DB.Url=jdbc:mariadb://localhost:3306/burstwallet
DB.Username=root
DB.Password=YOUR_PASSWORD
```

The MariaDB installation will also install HeidiSQL, a gui tool to administer MariaDb.
Use it to connect to the newly created mariaDb server and create a new DB called 'burstwallet'. 

#### Unix-like systems

Please install Java 8 (JRE 1.8) manually and run it by using burst.sh
You can get further information calling `burst.sh help`

A good HowTo for running the wallet on a mac can be found here
https://www.reddit.com/r/burstcoin/comments/7lrdc1/guide_to_getting_the_poc_wallet_running_on_a_mac/


##### Configure and Initialize MariaDB

The Debian and Ubuntu packages provide an automatic configuration of
your local mariadb server. If you can't use the packages, you have to
initialize your database with these statements:

```
echo "CREATE DATABASE burstwallet; 
      CREATE USER 'brs_user'@'localhost' IDENTIFIED BY 'yourpassword';
      GRANT ALL PRIVILEGES ON burstwallet.* TO 'brs_user'@'localhost';" | mysql -uroot
mysql -uroot burstwallet < init-mysql.sql
```

##### Configure your Wallet

Now you need to add the following stuff to your conf/brs.properties:

```
DB.Url=jdbc:mariadb://localhost:3306/burstwallet
DB.Username=brs_user
DB.Password=yourpassword
```

## Striking Features

- Proof of Capacity - ASIC proof / Energy efficient mining
- Fast sync. with multithread CPU or OpenCL/GPU (optional)
- Turing-complete smart contracts, via Automated Transactions (AT) https://ciyam.org/at/at.html
- Asset Exchange and Digital Goods Store
- Encrypted Messaging
- No ICO/Airdrops/Premine

## Specification

- 4 minute block time
- 2,158,812,800 coins total (see https://burstwiki.org/wiki/Block_Reward)
- Block reward starts at 10,000/block
- Block Reward Decreases at 5% each month

## Version History

For a general overview of Burst history see https://burstwiki.org/wiki/History_of_Burst

```
2018-03-15 2.0.0
           BRS - Burst Reference Software:
           Burst namespace, some NXT legacy is in API data sent P2P
           streamlined configuration namespace, more logical and intuitive
           migrated to JOOQ, supports many  DB backends; only H2 and mariaDB
           in-code to prevent bloat, all others via DB-manager
           UPnP functionality to help with router configuration for public nodes
           removed lots of unused code, updated many UI libraries
           significant improvements in P2P handling: re-sync speed, fork-handling
           peer acquisition
           Squashed many bugs and vulnerabilities, using subresource integrity
           test coverage went from 0% to over 20%

2017-10-28 1.3.6cg
           multi-DB support: added Firebird, re-added H2; support for quick
           binary dump and load

2017-09-04 1.3.4cg
           improved database deployment; bugfix: utf8 encoding

2017-08-11 1.3.2cg
           1st official PoCC release: MariaDB backend based on 1.2.9
```
For a detailed version history of wallets up to 1.2.9 see https://github.com/burst-team/burstcoin/releases

Code quality statistics can be found on SonarCloud: https://sonarcloud.io/dashboard?id=burstcoin%3Aburstcoin

## Build

Burstcoin can be built from source using maven or - preferably - via
the provided `burst.sh compile` script within this repository.

## Tools

To get rid of scalability and performance problems we use JProfiler as our
prefered <a href="https://www.ej-technologies.com/products/jprofiler/overview.html">Java
Profiler</a>.

## Credits

Numerous people have contributed to make this software what it is
today. The Nxt-developers before Burst was forked off from the Nxt
code base, the initial - yet anonymous - creator of burstcoin who did
carve out PoS and instantiate PoC and also many small contributors who
helped out with small fixes, typos, translations etc. In case we
forgot to mention someone, please do not hesitate to bring this to our
attention. Past contributions that have been removed from the code
base are not mentioned, however. In alphabetical order:

@4nt1g0
* helping with JOOQ migration

@ac0v
* initial replacement of the H2 wallet against mariaDB in the CG-lineage of the wallet
* introduction of FirebirdDB to the list of supported DB backends, bugfixing, debugging
* streamlining helper scripts (invocation, compilation)
* work on macOS port, testing and release management
* JOOQ migration and many more things

@Brabantian
* introduction of JUnit and Code Coverage reporting
* lots of unit tests and refactoring work to make the code testable

@BraindeadOne
* providing a DB abstraction layer to allow for multiple DB backends
* implementing a load/dump mechanism for faster sync (called Quicksync)
* added SQL metrics to the code to be able to spot DB performance problems
* CPU core limit patch, so the wallet can be assigned only part of your resources

@chrulri improved wallet behavior when running under https

@daWallet did many bugfixes and stabilization improvements

@dcct
* support for parallel blockchain downloads
* various bugfixes and lib updates

@de-luxe took care of the releases between 1.2.5 and 1.2.9

@fusecavator
* initial OpenCL code (GPU acceleration support)
* critical fixes to the wallet spam-attack vulnerability

@LithMage provided fixes and enhancements to the UI

@Quibus
* DownloadCache
* PoC2 implementation
* improved peer handling
* lots of bugfixes

@rico666
* moved the wallet from NRS/Nxt to BRS/Burst namespace
* improvements and fixes to the documentation - revival of javadoc references
* general code refactoring and styleguide unification (Google JAVA Styleguide)
* removed obsolete/unused code - tens of thousands of LOCs
* fixes and enhancements to the UI, config streamlining also JS updates

Other contributors

Accepted pull requests improving the wallet quality in several areas
were made by @Doncode, @naiduv



## Links

For further information, please visit the following pages.

Wiki: https://burstwiki.org (lots of other links)

Info: https://www.burst-coin.org/

Explorer: https://explore.burst.cryptoguru.org

Forum1: https://burstforum.net/

Forum2: https://forums.getburst.net/


### Bitcointalk

https://bitcointalk.org/index.php?topic=1541310 *(New unmoderated)*

https://bitcointalk.org/index.php?topic=1323657 *(Alternative moderated)*

https://bitcointalk.org/index.php?topic=731923 *(Original unmoderated)*

### Related repositories

https://github.com/rico666/cg_obup *CryptoGuru Optimized BURSTcoin Unix Plotter (Linux)*

https://github.com/Blagodarenko  *Blago's XPlotter, Windows Miner, PlotsChecker, etc.*

https://github.com/de-luxe *GPU assisted jMiner, Faucet Software, Observer, AddressGenerator*

https://github.com/Creepsky/creepMiner *C++ Crossplatform Miner*

https://github.com/BurstTools/BurstSoftware *Windows Plot Generator for SEE4/AVX2*

https://github.com/bhamon *gpuPlotGenerator, BurstMine (graphical plotter/miner)*

https://github.com/kartojal *GUI for Dcct Tools, GUI for gpuPlotGenerator (Linux)*

https://github.com/Kurairaito *Burst Plot Generator by Kurairaito*

https://github.com/uraymeiviar *C Miner, Pool, Block Explorer, Plot Composer (Linux)*

https://github.com/mrpsion/burst-mining-system *Web interface for Plotting and Mining*

### Additional Software
https://burstforum.net/category/9/burst-software
