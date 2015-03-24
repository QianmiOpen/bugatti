-- This SQL script creates the required tables by ch.qos.logback.classic.db.DBAppender.
DROP TABLE IF EXISTS `logging_event`;
CREATE TABLE `logging_event` (
  `timestmp` varchar(20) NOT NULL,
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
  FULLTEXT KEY `idx_fulltext` (`formatted_message`, `timestmp`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `logging_event_exception`;
CREATE TABLE `logging_event_exception` (
  `event_id` bigint(20) NOT NULL,
  `i` smallint(6) NOT NULL,
  `trace_line` varchar(254) NOT NULL,
  PRIMARY KEY (`event_id`,`i`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `logging_event_property`;
CREATE TABLE `logging_event_property` (
  `event_id` bigint(20) NOT NULL,
  `mapped_key` varchar(254) NOT NULL,
  `mapped_value` text,
  PRIMARY KEY (`event_id`,`mapped_key`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


INSERT INTO `app_user`(`job_no`, `name`, `role`, `password`, `locked`, `last_ip`, `last_visit`, `ssh_key`)
VALUES ('root', 'root', 'admin', 'dc76e9f0c0006e8f919e0c515c66dbba3982f785', 'n', '', now(), NULL);