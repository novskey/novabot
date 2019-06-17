-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               10.2.9-MariaDB - mariadb.org binary distribution
-- Server OS:                    Win64
-- HeidiSQL Version:             9.4.0.5125
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;

-- Dumping structure for table novabot.geocoding
CREATE TABLE IF NOT EXISTS `spawninfo` (
  `lat` double NOT NULL,
  `lon` double NOT NULL,
  `timezone` VARCHAR(30) DEFAULT NULL,
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

-- Data exporting was unselected.
-- Dumping structure for table novabot.pokemon
CREATE TABLE IF NOT EXISTS `pokemon` (
  `user_id` varchar(50) DEFAULT NULL,
  `id` varchar(30) DEFAULT NULL,
  `max_iv` float DEFAULT 100,
  `min_iv` float DEFAULT 0,
  `max_lvl` TINYINT DEFAULT 40,
  `min_lvl` TINYINT DEFAULT 0,
  `max_cp` INT DEFAULT 2147483647,
  `min_cp` INT DEFAULT 0,
  `location` varchar(30) DEFAULT NULL,
  PRIMARY KEY `pokemon_user_id_id_channel_max_iv_min_iv_pk` (`user_id`,`id`,`location`,`max_iv`,`min_iv`,`max_lvl`,`min_lvl`, `max_cp`,`min_cp`),
  CONSTRAINT `pokemon_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- New format:
CREATE TABLE IF NOT EXISTS `pokemon` (
  `user_id` varchar(50) DEFAULT NULL,
  `id` varchar(30) DEFAULT NULL,
  `max_iv` float DEFAULT 100,
  `min_iv` float DEFAULT 0,
  `max_lvl` TINYINT DEFAULT 40,
  `min_lvl` TINYINT DEFAULT 0,
  `max_cp` INT DEFAULT 2147483647,
  `min_cp` INT DEFAULT 0,
  `location` varchar(30) DEFAULT NULL,
  `min_attack_iv` INT DEFAULT 0,
  `max_attack_iv` INT DEFAULT 15,
  `min_defense_iv` INT DEFAULT 0,
  `max_defense_iv` INT DEFAULT 15,
  `min_stamina_iv` INT DEFAULT 0,
  `max_stamina_iv` INT DEFAULT 15,
  `pvp_great_rank` INT DEFAULT 4096,
  `pvp_ultra_rank` INT DEFAULT 4096
  PRIMARY KEY `pokemon_user_id_id_channel_max_iv_min_iv_pk` (`user_id`,`id`,`location`,`max_iv`,`min_iv`,`max_lvl`,`min_lvl`, `max_cp`,`min_cp`,`min_attack_iv`,`max_attack_iv`,`min_defense_iv`,`max_defense_iv`,`min_stamina_iv`,`max_stamina_iv`,`pvp_great_rank`,`pvp_ultra_rank`),
  CONSTRAINT `pokemon_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
-- Dumping structure for table novabot.preset
CREATE TABLE IF NOT EXISTS `preset` (
  `user_id` varchar(50) NOT NULL,
  `preset_name` varchar(50) NOT NULL,
  `location` varchar(50) NOT NULL,
  PRIMARY KEY (`user_id`,`preset_name`,`location`),
  CONSTRAINT `preset_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
-- Dumping structure for table novabot.raid
CREATE TABLE IF NOT EXISTS `raid` (
  `user_id` varchar(50) NOT NULL,
  `boss_id` int(11) NOT NULL,
  `location` varchar(30) NOT NULL,
  PRIMARY KEY (`user_id`,`boss_id`,`location`),
  CONSTRAINT `raid_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
-- Dumping structure for table novabot.raidlobby
CREATE TABLE IF NOT EXISTS `raidlobby` (
  `lobby_id` int(11) NOT NULL,
  `gym_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `members` int(11) DEFAULT 0,
  `role_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `channel_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `next_timeleft_update` int(11) DEFAULT NULL,
  `invite_code` varchar(10) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`lobby_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
-- Dumping structure for table novabot.users
CREATE TABLE IF NOT EXISTS `users` (
  `id` varchar(50) NOT NULL,
  `joindate` timestamp NOT NULL DEFAULT current_timestamp(),
  `paused` tinyint(1) DEFAULT 0,
  `bot_token` varchar(70) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `users_id_uindex` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Data exporting was unselected.
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IF(@OLD_FOREIGN_KEY_CHECKS IS NULL, 1, @OLD_FOREIGN_KEY_CHECKS) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
