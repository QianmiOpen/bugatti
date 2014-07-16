# Logback: the reliable, generic, fast and flexible logging framework.
# Copyright (C) 1999-2010, QOS.ch. All rights reserved.
#
# See http://logback.qos.ch/license.html for the applicable licensing
# conditions.
# This SQL script creates the required tables by ch.qos.logback.classic.db.DBAppender.
#
# It is intended for MySQL databases. It has been tested on MySQL 5.1.37
# on Linux

BEGIN;
DROP TABLE IF EXISTS `logging_event`;
DROP TABLE IF EXISTS `logging_event_exception`;
DROP TABLE IF EXISTS `logging_event_property`;
COMMIT;

BEGIN;
CREATE TABLE `logging_event` (
  `timestmp` bigint(20) NOT NULL,
  `formatted_message` text NOT NULL,
  `logger_name` varchar(254) NOT NULL,
  `level_string` varchar(254) NOT NULL,
  `thread_name` varchar(254) DEFAULT NULL,
  `reference_flag` smallint(6) DEFAULT NULL,
  `arg0` varchar(254) DEFAULT NULL,
  `arg1` varchar(254) DEFAULT NULL,
  `arg2` varchar(254) DEFAULT NULL,
  `arg3` varchar(254) DEFAULT NULL,
  `caller_filename` varchar(254) NOT NULL,
  `caller_class` varchar(254) NOT NULL,
  `caller_method` varchar(254) NOT NULL,
  `caller_line` char(4) NOT NULL,
  `event_id` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`event_id`),
  KEY `idx_time` (`timestmp`),
  FULLTEXT KEY `idx_fulltext` (`formatted_message`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
COMMIT;

BEGIN;
CREATE TABLE `logging_event_exception` (
  `event_id` bigint(20) NOT NULL,
  `i` smallint(6) NOT NULL,
  `trace_line` varchar(254) NOT NULL,
  PRIMARY KEY (`event_id`,`i`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
COMMIT;

BEGIN;
CREATE TABLE `logging_event_property` (
  `event_id` bigint(20) NOT NULL,
  `mapped_key` varchar(254) NOT NULL,
  `mapped_value` text,
  PRIMARY KEY (`event_id`,`mapped_key`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
COMMIT;
