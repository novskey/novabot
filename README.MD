# novabot

novabot uses Lombok

novabot is a [Discord](https://discordapp.com) bot that uses data from [RocketMap](https://github.com/RocketMap/RocketMap) and Monocle ([ZeChrales](https://github.com/ZeChrales/Monocle) and [Hydro74000](https://github.com/Hydro74000/Monocle) forks) to provide a complete, configurable package for Pokemon Go communities.
novabot's features include:

- Personalised pokemon and raid notifications, delivered by DM.
- Filter settings based on geofence, suburb, or IV.
- Automated posting of pokemon/raids to Discord channels.
- Unique raid coordination system.
- Optional tiered donator/supporter system using Discord roles to reward members of your community.

You can download the latest release [here](https://github.com/novskey/novabot/releases) (Just download the .jar file) 
Please see the [wiki](https://github.com/novskey/novabot/wiki) for more detailed information on configuration etc.

1. [Installation Guides](#install-guides)
    1. [Windows Installation](#windows-install)
        - [Install prerequisites](#win-prereqs)
            - [Java JRE](#win-java)
            - [MariaDB](#win-mariadb)
        - [Create a Discord bot](#win-create-bot)
        - [Create a new database](#win-create-db)
        - [Download and configure novabot](#win-configure-novabot)
        - [Running novabot](#win-running-novabot)
    2. [Linux Installation](#linux-install)
        - [Install prerequisites](#linux-prereqs)
            - [Java JRE](#linux-java)
            - [MariaDB](#linux-mariadb)
        - [Create a Discord bot](#linux-create-bot)
        - [Create a new database](#linux-create-db)
        - [Download and configure novabot](#linux-configure-novabot)
        - [Running novabot](#linux-running-novabot)
    3. [Mac Installation](#mac-install)
        - [Install prerequisites](#mac-prereqs)
            - [Java JRE](#mac-java)
            - [MariaDB](#mac-mariadb)
        - [Create a Discord bot](#mac-create-bot)
        - [Create a new database](#mac-create-db)
        - [Download and configure novabot](#mac-configure-novabot)
        - [Running novabot](#mac-running-novabot)
2. [Quick Start Guide](#quick-start)
3. [Discord Support Server](https://discord.gg/P3qrvkp)

### Prerequisites
- [Java JRE 64-bit](https://java.com/en/download/manual.jsp)
- [MySQL/MariaDB](https://mariadb.org/download/)

That's it!
## <a name="install-guides"></a> Installation Guides

### <a name="windows-install"></a>Windows

#### <a name="win-prereqs"></a>Install prerequisites

##### <a name="win-java"></a> Java JRE 64-bit
Download and run the offline 64-bit installer from https://java.com/en/download/manual.jsp. 
Make sure you select the 64-bit installer or novabot will not work correctly.

##### <a name="win-mariadb"></a> MariaDB
Follow the instructions [here](https://mariadb.org/download/) to install MariaDB.

#### <a name="win-create-bot"></a> Create a discord bot, add it to your server, and get the bot token
Follow [this guide](https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token) to create a discord bot, add it to your server, and get its bot token.

#### <a name="win-create-db"></a>Create a new database

In your MariaDB/MySQL terminal, use the below command to create a new database, where name is the desired name of the new database.

`create database name;`

Once you've created the database, select the database:

`use name;`

Next, run the DB creation script that can be found [here](create-db.sql), either by copy and posting the contents of the file into the terminal, or with the `source` command like so:

`source /path/to/file/create-db.sql;`

#### <a name="win-configure-novabot"></a>Download and configure novabot

You can find the latest novabot release [here](https://github.com/novskey/novabot/releases).
Each release includes all the necessary configuration and data files that novabot needs.
Once you've downloaded a release, see the wiki on [configuration](https://github.com/novskey/novabot/wiki/configuration) on how to configure novabot.

#### <a name="win-running-novabot"></a>Running novabot

Run novabot from cmd.exe (or alternative shell) with:

`java -jar novabot.jar`

### <a name="linux-install"></a> Linux

Tested on Ubuntu 16.04

#### <a name="linux-prereqs"></a>Install prerequisites

##### <a name="linux-java"></a>Java JRE

Install Java8 through terminal with

```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
```

##### <a name="linux-mariadb"></a>MariaDB

Follow the instructions [here](https://downloads.mariadb.org/mariadb/repositories/) to install MariaDB on your server.

#### <a name="linux-create-bot"></a>Create a discord bot, add it to your server, and get the bot token

Follow [this guide](https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token) to create a discord bot, add it to your server, and get its bot token.

#### <a name="linux-create-db"></a>Create a new database

In your MariaDB/MySQL terminal, use the below command to create a new database, where name is the desired name of the new database.

`create database name;`

Once you've created the database, select the database:

`use name;`

Next, run the DB creation script that can be found [here](create-db.sql), either by copy and posting the contents of the file into the terminal, or with the `source` command like so:

`source /path/to/file/create-db.sql;`

#### <a name="linux-configure-novabot"></a>Download and configure novabot

You can find the latest novabot release [here](https://github.com/novskey/novabot/releases).
Each release includes all the necessary configuration and data files that novabot needs.
Once you've downloaded a release, see the wiki on [configuration](https://github.com/novskey/novabot/wiki/configuration) on how to configure novabot.

#### <a name="linux-running-novabot"></a>Running novabot

Run novabot from a terminal with:

`java -jar novabot.jar`
### <a name="mac-install"></a> Mac

#### <a name="mac-prereqs"></a> Install prerequisites

##### <a name="mac-java"></a>Java JRE

Follow the instructions [here](https://java.com/en/download/help/mac_install.xml) to install Java.

##### <a name="mac-mariadb"></a>MariaDB
Follow the instructions [here](https://mariadb.org/download/) to install MariaDB.

#### <a name="mac-create-bot"></a>Create a discord bot, add it to your server, and get the bot token

Follow [this guide](https://github.com/reactiflux/discord-irc/wiki/Creating-a-discord-bot-&-getting-a-token) to create a discord bot, add it to your server, and get its bot token.

#### <a name="mac-create-db"></a> Create a new database

In your MariaDB/MySQL terminal, use the below command to create a new database, where name is the desired name of the new database.

`create database name;`

Once you've created the database, select the database:

`use name;`

Next, run the DB creation script that can be found [here](create-db.sql), either by copy and posting the contents of the file into the terminal, or with the `source` command like so:

`source /path/to/file/create-db.sql;`

#### <a name="mac-configure-novabot"></a>Download and configure novabot

You can find the latest novabot release [here](https://github.com/novskey/novabot/releases).
Each release includes all the necessary configuration and data files that novabot needs.
Once you've downloaded a release, see the wiki on [configuration](https://github.com/novskey/novabot/wiki/configuration) on how to configure novabot.

#### <a name="mac-running-novabot"></a>Running novabot

Run novabot from a terminal with:

`java -jar novabot.jar`

## <a name="quick-start"></a> Quick Start Guide
TBD
