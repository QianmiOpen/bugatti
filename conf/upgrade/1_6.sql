ALTER TABLE `project` ADD COLUMN `description` VARCHAR(500) NULL COMMENT '项目描述' AFTER `name`;

CREATE TABLE `template_dependence` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `template_id` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `description` varchar(128),
  `default_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tempid_name` (`template_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
