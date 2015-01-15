rename table `task_template` to `template_action`;

rename table `task_template_step` to `template_action_step`;

ALTER TABLE `template_action_step` ADD COLUMN `do_if` VARCHAR(2000) NULL COMMENT 'do if 表达式';


CREATE TABLE `spirit` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `ip` varchar(16) NOT NULL,
  `info` varchar(256),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


rename table `environment_project_rel` to `host`;

ALTER TABLE `host` ADD COLUMN `spirit_id` int(11) NOT NULL COMMENT 'spirit id' AFTER `syndic_name`;

ALTER TABLE `host` ADD COLUMN `state` int(11) NOT NULL COMMENT 'state[0:noKey, 1:offline, 2:arrived, 3:online]' AFTER `ip`;