CREATE TABLE IF NOT EXISTS `saga_event_entity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `saga_id` varchar(36) NOT NULL,
  `creation_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `type` varchar(50) NOT NULL,
  `content_json` JSON NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `running_sagas_index` (`saga_id`, `type`)
) DEFAULT CHARSET=utf8;
