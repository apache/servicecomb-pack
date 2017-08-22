DROP TABLE IF EXISTS saga_event_entity;

CREATE TABLE `saga_event_entity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `saga_id` varchar(36) NOT NULL,
  `creation_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `type` varchar(50) NOT NULL,
  `content_json` clob NOT NULL DEFAULT '{}',
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

 CREATE INDEX running_sagas_index ON saga_event_entity(saga_id, type);
