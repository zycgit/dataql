CREATE TABLE IF NOT EXISTS `dw_auth_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `role_name` varchar(127) NOT NULL,
  `role_auth_labels` longtext NOT NULL,
  `alias_name` varchar(127) DEFAULT NULL,
  `inner_tag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `dw_auth_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `uid` varchar(127) NOT NULL,
  `username` varchar(255) NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `phone` varchar(128) DEFAULT NULL,
  `account` varchar(128) DEFAULT NULL,
  `password` varchar(512) NOT NULL,
  `role_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_dw_auth_user_uid` UNIQUE (`uid`)
);

CREATE INDEX `idx_dw_auth_user_username` ON `dw_auth_user` (`username`);
CREATE INDEX `idx_dw_auth_user_account` ON `dw_auth_user` (`account`);
CREATE INDEX `idx_dw_auth_user_phone` ON `dw_auth_user` (`phone`);
CREATE INDEX `idx_dw_auth_user_email` ON `dw_auth_user` (`email`);
CREATE INDEX `idx_dw_auth_user_role_id` ON `dw_auth_user` (`role_id`);

CREATE TABLE IF NOT EXISTS `dw_ds` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ds_type` varchar(128) NOT NULL,
  `name` varchar(255) NOT NULL,
  `desc` varchar(512) DEFAULT NULL,
  `display_host` varchar(512) DEFAULT NULL,
  `ds_env_id` bigint DEFAULT NULL,
  `bind_cluster_id` bigint DEFAULT NULL,
  `db_version` varchar(255) DEFAULT NULL,
  `driver_version` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `idx_dw_ds_name` ON `dw_ds` (`name`);
CREATE INDEX `idx_dw_ds_env_id` ON `dw_ds` (`ds_env_id`);
CREATE INDEX `idx_dw_ds_cluster_id` ON `dw_ds` (`bind_cluster_id`);

CREATE TABLE IF NOT EXISTS `dw_ds_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ds_id` bigint NOT NULL,
  `config_name` varchar(64) NOT NULL,
  `config_value` longtext DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `idx_dw_ds_config_name` ON `dw_ds_config` (`ds_id`, `config_name`);
CREATE INDEX `idx_dw_ds_config_ds_id` ON `dw_ds_config` (`ds_id`);

CREATE TABLE IF NOT EXISTS `dw_interface_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `api_id` varchar(64) NOT NULL,
  `api_method` varchar(12) NOT NULL,
  `api_path` varchar(512) NOT NULL,
  `api_status` varchar(4) NOT NULL,
  `api_comment` varchar(255) NOT NULL,
  `api_type` varchar(24) NOT NULL,
  `api_script` mediumtext NOT NULL,
  `api_schema` mediumtext NOT NULL,
  `api_sample` mediumtext NOT NULL,
  `api_option` mediumtext NOT NULL,
  `api_create_time` bigint NOT NULL,
  `api_gmt_time` bigint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_dw_interface_info_api_id` UNIQUE (`api_id`),
  CONSTRAINT `uk_dw_interface_info_path` UNIQUE (`api_path`)
);

CREATE TABLE IF NOT EXISTS `dw_interface_history` (
  `history_id` varchar(64) NOT NULL,
  `history_api_id` varchar(64) NOT NULL,
  `history_method` varchar(12) NOT NULL,
  `history_path` varchar(512) NOT NULL,
  `history_status` varchar(4) NOT NULL,
  `history_comment` varchar(255) NOT NULL,
  `history_type` varchar(24) NOT NULL,
  `history_script` mediumtext NOT NULL,
  `history_script_ori` mediumtext NOT NULL,
  `history_schema` mediumtext NOT NULL,
  `history_sample` mediumtext NOT NULL,
  `history_option` mediumtext NOT NULL,
  `history_create_time` bigint NOT NULL,
  `is_release` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`history_id`)
);

CREATE INDEX `idx_dw_interface_history_api` ON `dw_interface_history` (`history_api_id`);
CREATE INDEX `idx_dw_interface_history_path` ON `dw_interface_history` (`history_path`);
