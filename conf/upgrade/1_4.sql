ALTER TABLE `variable` MODIFY `value` VARCHAR(2048);

ALTER TABLE `variable` ADD COLUMN `level` INT(10) NOT NULL DEFAULT 0 COMMENT '属性级别,默认不安全:0' AFTER `value`;

UPDATE `variable` SET `level` = 0;
