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

ALTER TABLE `logging_event` DROP INDEX `idx_fulltext`;
ALTER TABLE `logging_event` CHANGE COLUMN `timestmp` `timestmp` bigint(20) NOT NULL first;
ALTER TABLE `logging_event` ADD INDEX `idx_time` USING BTREE (`timestmp`);
ALTER TABLE `logging_event` ADD FULLTEXT `idx_fulltext` (`formatted_message`);

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
