CREATE TABLE `component_md5sum` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `script_type` int(11) NOT NULL,
  `script_version_id` int(11) NOT NULL,
  `component_name` varchar(60) NOT NULL,
  `md5sum` varchar(256) NOT NULL,
  `update_time` timestamp NOT NULL DEFAULT '2015-02-07 17:29:55',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;