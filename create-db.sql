-- MySQL dump 10.14  Distrib 5.5.56-MariaDB, for Linux (x86_64)
--
-- Host: localhost    Database: novabotdb
-- ------------------------------------------------------
-- Server version	5.5.56-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `pokemon`
--

DROP TABLE IF EXISTS `pokemon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pokemon` (
  `user_id` varchar(50) NOT NULL DEFAULT '',
  `id` varchar(30) NOT NULL DEFAULT '',
  `max_iv` float NOT NULL DEFAULT '100',
  `min_iv` float NOT NULL DEFAULT '0',
  `max_lvl` tinyint(4) NOT NULL DEFAULT '40',
  `min_lvl` tinyint(4) NOT NULL DEFAULT '0',
  `max_cp` int(11) NOT NULL DEFAULT '2147483647',
  `min_cp` int(11) NOT NULL DEFAULT '0',
  `location` varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (`user_id`,`id`,`location`,`max_iv`,`min_iv`,`max_lvl`,`min_lvl`,`max_cp`,`min_cp`),
  CONSTRAINT `pokemon_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `preset`
--

DROP TABLE IF EXISTS `preset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `preset` (
  `user_id` varchar(50) NOT NULL,
  `preset_name` varchar(50) NOT NULL,
  `location` varchar(50) NOT NULL,
  PRIMARY KEY (`user_id`,`preset_name`,`location`),
  CONSTRAINT `preset_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `raid`
--

DROP TABLE IF EXISTS `raid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `raid` (
  `user_id` varchar(50) NOT NULL,
  `boss_id` int(11) NOT NULL,
  `egg_level` tinyint(4) NOT NULL DEFAULT '0',
  `gym_name` varchar(100) NOT NULL DEFAULT '',
  `location` varchar(30) NOT NULL,
  `raid_level` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`,`boss_id`,`egg_level`,`raid_level`,`location`,`gym_name`),
  CONSTRAINT `raid_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `raidlobby`
--

DROP TABLE IF EXISTS `raidlobby`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `raidlobby` (
  `lobby_id` int(11) NOT NULL,
  `gym_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `role_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `channel_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `next_timeleft_update` int(11) DEFAULT NULL,
  `invite_code` varchar(10) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`lobby_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `raidlobby_lobbychat`
--

DROP TABLE IF EXISTS `raidlobby_lobbychat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `raidlobby_lobbychat` (
  `message_id` varchar(50) NOT NULL,
  `lobby_id` int(11) NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `raidlobby_lobbychat_lobby_id_fk` (`lobby_id`),
  CONSTRAINT `raidlobby_lobbychat_lobby_id_fk` FOREIGN KEY (`lobby_id`) REFERENCES `raidlobby` (`lobby_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `raidlobby_members`
--

DROP TABLE IF EXISTS `raidlobby_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `raidlobby_members` (
  `lobby_id` int(11) NOT NULL,
  `user_id` varchar(50) NOT NULL,
  `count` int(11) DEFAULT '1',
  `time` varchar(5) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`lobby_id`,`user_id`),
  CONSTRAINT `raidlobby_members_lobby_id_fk` FOREIGN KEY (`lobby_id`) REFERENCES `raidlobby` (`lobby_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spawninfo`
--

DROP TABLE IF EXISTS `spawninfo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spawninfo` (
  `lat` double NOT NULL,
  `lon` double NOT NULL,
  `timezone` varchar(30) DEFAULT NULL,
  `suburb` varchar(30) DEFAULT NULL,
  `street_num` varchar(15) DEFAULT NULL,
  `street` varchar(100) DEFAULT NULL,
  `state` varchar(30) DEFAULT NULL,
  `postal` varchar(15) DEFAULT NULL,
  `neighbourhood` varchar(50) DEFAULT NULL,
  `sublocality` varchar(50) DEFAULT NULL,
  `country` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`lat`,`lon`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `token`
--

DROP TABLE IF EXISTS `token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `token` (
  `token` varchar(128) NOT NULL,
  `valid_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` varchar(50) NOT NULL,
  PRIMARY KEY (`token`),
  KEY `token_users_id_fk` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` varchar(50) NOT NULL,
  `joindate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `paused` tinyint(1) DEFAULT '0',
  `bot_token` varchar(70) DEFAULT NULL,
  `verified` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `users_id_uindex` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-07-07  4:37:05
