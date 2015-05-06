ALTER TABLE `host` ADD COLUMN `ip_clash` INT(10) NOT NULL DEFAULT 0 COMMENT 'ip冲突辅助字段，同ip做联合主机，默认0' AFTER `ip`;

ALTER TABLE `host` DROP INDEX `idx_ip`;

ALTER TABLE `host` ADD UNIQUE `idx_ip` USING BTREE (`ip`, `ip_clash`);
