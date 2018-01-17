DROP TABLE IF EXISTS SagaEventEntity;

CREATE TABLE `SagaEventEntity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sagaId` varchar(36) NOT NULL,
  `creationTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `type` varchar(50) NOT NULL,
  `contentJson` clob NOT NULL DEFAULT '{}',
  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8;

 CREATE INDEX running_sagas_index ON sagaEventEntity(sagaId, type);
