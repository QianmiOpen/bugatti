
CREATE TABLE `area_environment_rel` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `area_id` int(11) NOT NULL,
  `env_id` int(11) NOT NULL,
  `ip_range` varchar(300) NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_area_env` (`area_id`,`env_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `environment_project_rel` ADD COLUMN `container_type` ENUM('vm', 'docker') NOT NULL DEFAULT 'vm' COMMENT '容器类型' AFTER `ip`;
ALTER TABLE `environment_project_rel` ADD COLUMN `area_id` INT(30) NULL COMMENT '区域id' AFTER `project_id`;
ALTER TABLE `environment_project_rel` ADD COLUMN `host_ip` VARCHAR(50) NULL COMMENT '物理机ip' AFTER `container_type`;
ALTER TABLE `environment_project_rel` ADD COLUMN `host_name` VARCHAR(100) NULL COMMENT '物理机name' AFTER `host_ip`;

