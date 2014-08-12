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