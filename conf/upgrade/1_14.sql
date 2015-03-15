ALTER TABLE `app_user` ADD COLUMN `password` VARCHAR(256) NULL COMMENT '用户密码，默认仅管理有' AFTER `role`;

ALTER TABLE `app_user` DROP `super_admin`;

INSERT INTO `app_user`(`job_no`, `name`, `role`, `password`, `locked`, `last_ip`, `last_visit`, `ssh_key`)
VALUES ('root', 'root', 'admin', 'dc76e9f0c0006e8f919e0c515c66dbba3982f785', 'n', '', now(), NULL);

DROP TABLE `permission`;

UPDATE `app_user` SET `role` = 'user' WHERE `job_no` != 'root';

ALTER TABLE `environment_member` ADD COLUMN `level` INT NOT NULL DEFAULT 0 COMMENT '环境成员级别,1:safe,0:unsafe' AFTER `env_id`;

INSERT INTO environment_member(`env_id`, `level`, `job_no`)
  SELECT `id`, 1, `job_no`
  FROM `environment`
  WHERE `job_no` is not NULL;

ALTER TABLE `environment` DROP `job_no`;
