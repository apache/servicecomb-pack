DROP TABLE IF EXISTS saga_event_entity;

CREATE TABLE `saga_event_entity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `saga_id` varchar(36) NOT NULL,
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `type` varchar(50) NOT NULL,
  `content_json` varchar(1000) NOT NULL DEFAULT '{}',
  PRIMARY KEY (`id`)
--  INDEX (`saga_id`, `type`)
) DEFAULT CHARSET=utf8;
