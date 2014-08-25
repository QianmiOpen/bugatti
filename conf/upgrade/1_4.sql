ALTER TABLE `environment` DROP `global_variable`;

ALTER TABLE `project` DROP `global_variable`;

CREATE TABLE `variable` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `env_id` int(11) NOT NULL,
  `project_id` int(11) NOT NULL,
  `name` varchar(254) NOT NULL,
  `value` varchar(254) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_name` (`env_id`,`project_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `project_dependency` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` int(11) NOT NULL,
  `dependency_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

ALTER TABLE `project_dependency` ADD UNIQUE `idx_relation` USING BTREE (`project_id`, `dependency_id`);

insert into project_dependency(project_id, dependency_id) select id, -1 from project;

ALTER TABLE `template_item` ADD COLUMN `item_type` ENUM('attr', 'var') NOT NULL DEFAULT 'attr' COMMENT '类型' AFTER `item_desc`;

ALTER TABLE `template_item` DROP INDEX `idx_tid_order`;
ALTER TABLE `template_item` ADD INDEX `idx_tid_order` USING BTREE(`template_id`, `script_version`, `order`);


CREATE TABLE template_alias
(
  `id` INT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `template_id` INT NOT NULL,
  `name` VARCHAR(254) NOT NULL,
  `value` VARCHAR(254) NOT NULL,
  `description` VARCHAR(254) NOT NULL,
  `script_version` VARCHAR(254) DEFAULT 'master' NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `task_template` ADD COLUMN `action_type` ENUM('project', 'host') NOT NULL DEFAULT 'project' COMMENT '动作类型';

ALTER TABLE `environment_project_rel` ADD COLUMN `global_variable` TEXT NOT NULL COMMENT '机器变量';

ALTER TABLE `template` ADD COLUMN `dependent_project` VARCHAR(254) NOT NULL DEFAULT '' COMMENT '模板依赖项目id列表';