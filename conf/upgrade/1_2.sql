-- 配置文件增加文件类型
ALTER TABLE `conf` DROP INDEX `idx_path`;
ALTER TABLE `conf` ADD COLUMN `file_type` VARCHAR(50) NULL COMMENT '文件类型' AFTER `path`;
ALTER TABLE `conf` CHANGE `name` `file_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '文件名称';
ALTER TABLE `conf` CHANGE `path` `file_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '文件路径';
ALTER TABLE `conf` ADD UNIQUE `idx_path` USING BTREE (`env_id`, `version_id`, `file_path`);

ALTER TABLE `conf_log` ADD COLUMN `file_type` VARCHAR(50) NULL COMMENT '文件类型' AFTER `path`;
ALTER TABLE `conf_log` CHANGE `name` `file_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '文件名称';
ALTER TABLE `conf_log` CHANGE `path` `file_path` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '文件路径';

ALTER TABLE `app_user` CHANGE COLUMN `name` `name` varchar(20) NOT NULL;

ALTER TABLE `environment` ADD COLUMN `script_version` varchar(254) NOT NULL DEFAULT 'latest' after `level`;

ALTER TABLE `environment_project_rel` DROP INDEX `idx_eid_pid`;
ALTER TABLE `environment_project_rel` DROP INDEX `idx_ip`;

CREATE TABLE `script_version` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `update_time` timestamp NOT NULL DEFAULT '2014-07-26 14:14:53',
  `message` varchar(254) DEFAULT NULL,
  PRIMARY KEY (`id`)) ENGINE=`InnoDB`;

ALTER TABLE `task_template` ADD COLUMN `script_version` varchar(254) NOT NULL DEFAULT 'master' after `order_num`;

ALTER TABLE `template_item` DROP INDEX `idx_name`;
ALTER TABLE `template_item` ADD COLUMN `script_version` varchar(254) NOT NULL DEFAULT 'master' after `order`;
ALTER TABLE `template_item` ADD UNIQUE `idx_name` USING BTREE (`template_id`, `item_name`, `script_version`);

DROP TABLE `logging_event`;
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

DROP TABLE `logging_event_exception`;
CREATE TABLE `logging_event_exception` (
  `event_id` bigint(20) NOT NULL,
  `i` smallint(6) NOT NULL,
  `trace_line` varchar(254) NOT NULL,
  PRIMARY KEY (`event_id`,`i`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

DROP TABLE `logging_event_property`;
CREATE TABLE `logging_event_property` (
  `event_id` bigint(20) NOT NULL,
  `mapped_key` varchar(254) NOT NULL,
  `mapped_value` text,
  PRIMARY KEY (`event_id`,`mapped_key`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

ALTER TABLE `project` ADD COLUMN `global_variable` TEXT NULL COMMENT '项目全局变量';
ALTER TABLE `environment` ADD COLUMN `global_variable` TEXT NULL COMMENT '环境全局变量';
