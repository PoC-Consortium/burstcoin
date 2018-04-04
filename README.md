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
- MariaDB (recommended)
- H2 (embedded, easier install)

Other DB backends are supported by the Burstcoin DB manager:
<https://github.com/PoC-Consortium/burstcoin-db-manager>


### Software Installation

#### Linux (Debian, Ubuntu)

Please take a look at <http://package.cryptoguru.org/> where you can
find the burstcoincg package. This will take care of the MariaDB
installation for you.

Burstcoin can be built from source using maven or - preferably - via
the provided `burst.sh compile` script within this repository.

##### Upgrading your wallet config from 1.3.6cg

```
burst.sh upgrade
```
will take the old `nxt-default.properties`/`nxt.properties` files and
create `brs-default.properties.converted`/`brs.properties.converted`
files in the conf directory. This should give you a headstart with the
new option naming system.

#### Windows

###### MariaDb

In the conf directory, copy brs-default.properties into a new file named brs.properties.

Download and install MariaDB <https://mariadb.com/downloads/mariadb-tx>

The MariaDb installation will ask to setup a password for the root user. 
Add this password to the brs.properties file created above in the following section:
```
DB.Url=jdbc:mariadb://localhost:3306/brs_master
DB.Username=root
DB.Password=YOUR_PASSWORD
```

The MariaDB installation will also install HeidiSQL, a gui tool to administer MariaDb.
Use it to connect to the newly created mariaDb server and create a new DB called 'burstwallet'. 

#### Unix-like systems

Please install Java 8 (JRE 1.8) manually and run it by using burst.sh
You can get further information calling `burst.sh help`

A good HowTo for running the wallet on a mac can be found here
<https://www.reddit.com/r/burstcoin/comments/7lrdc1/guide_to_getting_the_poc_wallet_running_on_a_mac/>


##### Configure and Initialize MariaDB

The Debian and Ubuntu packages provide an automatic configuration of
your local mariadb server. If you can't use the packages, you have to
initialize your database with these statements:

```
echo "CREATE DATABASE brs_master; 
      CREATE USER 'brs_user'@'localhost' IDENTIFIED BY 'yourpassword';
      GRANT ALL PRIVILEGES ON brs_master.* TO 'brs_user'@'localhost';" | mysql -uroot
mysql -uroot < init-mysql.sql
```

##### Configure your Wallet

Now you need to add the following stuff to your conf/brs.properties:

```
DB.Url=jdbc:mariadb://localhost:3306/brs_master
DB.Username=brs_user
DB.Password=yourpassword
```

## Striking Features

- Proof of Capacity - ASIC proof / Energy efficient mining
- Fast sync. with multithread CPU or OpenCL/GPU (optional)
- Turing-complete smart contracts, via Automated Transactions (AT) <https://ciyam.org/at/at.html>
- Asset Exchange and Digital Goods Store
- Encrypted Messaging
- No ICO/Airdrops/Premine

## Specification

- 4 minute block time
- 2,158,812,800 coins total (see <https://burstwiki.org/wiki/Block_Reward>)
- Block reward starts at 10,000/block
- Block Reward Decreases at 5% each month

## [Version History](doc/History.md)

## Tools

To improve scalability and performance, the core development team uses
<a href="https://www.ej-technologies.com/products/jprofiler/overview.html">JProfiler</a>
as its preferred Java Profiler.

## [Known Issues](doc/KnownIssues.md)
## [Development Info](doc/Refactoring.md)
## [Credits](doc/Credits.md)
## [References/Links](doc/References.md)
