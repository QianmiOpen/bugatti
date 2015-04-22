
ALTER TABLE `host` ADD COLUMN `pre_project_id` Int(10) NULL COMMENT '之前绑定的项目id' AFTER `project_id`;
