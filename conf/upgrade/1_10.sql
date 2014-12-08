ALTER TABLE `app_user` ADD COLUMN `ssh_key` VARCHAR(1024) NULL COMMENT '用户ssh key';

UPDATE permission SET functions = concat('1,', functions) WHERE functions NOT LIKE '%1%' AND functions != '';

UPDATE permission SET functions = concat('1', functions) WHERE functions = '';