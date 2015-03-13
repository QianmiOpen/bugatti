ALTER TABLE `app_user` ADD COLUMN `password` VARCHAR(256) NULL COMMENT '用户密码，默认仅管理有' AFTER `role`;

ALTER TABLE `app_user` DROP `super_admin`;

INSERT INTO `app_user`(`job_no`, `name`, `role`, `password`, `locked`, `last_ip`, `last_visit`, `ssh_key`)
VALUES ('root', 'root', 'admin', 'dc76e9f0c0006e8f919e0c515c66dbba3982f785', 'n', '', now(), NULL);

DROP TABLE `permission`;

UPDATE `app_user` SET `role` = 'user' WHERE `job_no` != 'root';