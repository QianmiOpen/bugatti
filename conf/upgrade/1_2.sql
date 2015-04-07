-- 更新现有的template_action为负载级别
update template_action t set t.action_type = 'host';

ALTER TABLE `host` MODIFY COLUMN `ip` VARCHAR(255) NULL;