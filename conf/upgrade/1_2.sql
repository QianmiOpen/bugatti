-- 配置文件增加文件类型
ALTER TABLE `conf` ADD COLUMN `file_type` VARCHAR(50) NULL COMMENT '文件类型' AFTER `path`;
ALTER TABLE `conf` CHANGE `name` `file_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '文件名称';
ALTER TABLE `conf` CHANGE `path` `file_path` VARCHAR(300) NOT NULL DEFAULT '' COMMENT '文件路径';

ALTER TABLE `conf_log` ADD COLUMN `file_type` VARCHAR(50) NULL COMMENT '文件类型' AFTER `path`;
ALTER TABLE `conf_log` CHANGE `name` `file_name` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '文件名称';
ALTER TABLE `conf_log` CHANGE `path` `file_path` VARCHAR(300) NOT NULL DEFAULT '' COMMENT '文件路径';


