CREATE TABLE IF NOT EXISTS TxEvent (
  surrogateId BIGSERIAL PRIMARY KEY,
  serviceName varchar(16) NOT NULL,
  instanceId varchar(36) NOT NULL,
  creationTime timestamp(6) NOT NULL DEFAULT CURRENT_DATE,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  type varchar(50) NOT NULL,
  compensationMethod varchar(256) NOT NULL,
  payloads bytea
);

CREATE INDEX IF NOT EXISTS saga_events_index ON TxEvent (surrogateId, globalTxId, localTxId, type);


CREATE TABLE IF NOT EXISTS Command (
  surrogateId BIGSERIAL PRIMARY KEY,
  eventId bigint NOT NULL UNIQUE,
  serviceName varchar(16) NOT NULL,
  instanceId varchar(36) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  compensationMethod varchar(256) NOT NULL,
  payloads bytea,
  status varchar(12),
  lastModified timestamp(6) NOT NULL DEFAULT CURRENT_DATE,
  version bigint NOT NULL
);

CREATE INDEX IF NOT EXISTS saga_commands_index ON Command (surrogateId, eventId, globalTxId, localTxId, status);
