ALTER TABLE `environment` ADD COLUMN locked ENUM('y', 'n') NOT NULL DEFAULT 'n'  COMMENT '是否锁定环境,默认不锁定:n' AFTER `level`;
