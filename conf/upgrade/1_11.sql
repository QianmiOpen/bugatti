rename table `task_template` to `template_action`;

rename table `task_template_step` to `template_action_step`;

ALTER TABLE `template_action_step` ADD COLUMN `do_if` VARCHAR(2000) NULL COMMENT 'do if 表达式';


CREATE TABLE `spirit` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL,
  `ip` varchar(16) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


rename table `environment_project_rel` to `host`;
