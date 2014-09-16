
ALTER TABLE `environment` ADD COLUMN `job_no` VARCHAR(16) NULL COMMENT '创建人' AFTER `name`;

ALTER TABLE `member` RENAME `project_member`;

CREATE TABLE `environment_member` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `env_id` int(11) NOT NULL,
  `job_no` varchar(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_eid_no` (`env_id`,`job_no`),
  KEY `idx_eid` (`env_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `task_queue` ADD COLUMN `cluster_name` VARCHAR(254) NULL COMMENT '负载机器名称' AFTER `project_id`;

ALTER TABLE `task` ADD COLUMN `cluster_name` VARCHAR(254) NULL COMMENT '负载机器名称' AFTER `project_id`;