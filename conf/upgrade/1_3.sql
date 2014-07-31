ALTER TABLE `conf_content` ADD COLUMN `octet` BIT NOT NULL DEFAULT 0 COMMENT '二进制文件标识,0:普通文件,1:二进制文件' AFTER `conf_id`;
ALTER TABLE `conf_content` CHANGE `content` `content` MEDIUMBLOB NOT NULL COMMENT '文件内容改为二进制类型';

ALTER TABLE `conf_log_content` ADD COLUMN `octet` BIT NOT NULL DEFAULT 0 COMMENT '二进制文件标识,0:普通文件,1:二进制文件' AFTER `conf_log_id`;
ALTER TABLE `conf_log_content` CHANGE `content` `content` MEDIUMBLOB NOT NULL COMMENT '文件内容改为二进制类型';