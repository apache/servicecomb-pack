CREATE TABLE IF NOT EXISTS `tx_event_envelope` (
  `surrogate_id` bigint NOT NULL AUTO_INCREMENT,
  `service_name` varchar(16) NOT NULL,
  `instance_id` varchar(36) NOT NULL,
  `creation_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `global_tx_id` varchar(36) NOT NULL,
  `local_tx_id` varchar(36) NOT NULL,
  `parent_tx_id` varchar(36) DEFAULT NULL,
  `type` varchar(50) NOT NULL,
  `compensation_method` varchar(256) NOT NULL,
  `payloads` varbinary(10240),
  PRIMARY KEY (`surrogate_id`),
  INDEX `running_sagas_index` (`global_tx_id`, `local_tx_id`, `type`)
) DEFAULT CHARSET=utf8;
