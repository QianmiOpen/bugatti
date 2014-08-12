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