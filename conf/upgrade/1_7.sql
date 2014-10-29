CREATE TABLE `template_dependence` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `template_id` int(11) NOT NULL,
  `name` varchar(64) NOT NULL,
  `description` varchar(128),
  `default_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_tempid_name` (`template_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `template_dependence` ADD COLUMN `dependency_type` VARCHAR(64) NOT NULL COMMENT '依赖项目的类型名称' AFTER `name`;

ALTER TABLE `project_dependency` ADD COLUMN `alias` VARCHAR(64) NULL COMMENT '项目别名，默认为项目名称，模板依赖时为模板中指定别名' AFTER `dependency_id`;

update conf_content set content = replace(content, 'dependence.LogServer', 'dependence.$logServer');

update conf_content set content = replace(content, 'dependence.ZookeeperRegister', 'dependence.$dubboRegister');

update conf_content set content = replace(content, 'dependence.ForestTengine', 'dependence.$frontTengine');