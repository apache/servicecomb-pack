CREATE TABLE IF NOT EXISTS TxEvent (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  serviceName varchar(16) NOT NULL,
  instanceId varchar(36) NOT NULL,
  creationTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  type varchar(50) NOT NULL,
  compensationMethod varchar(256) NOT NULL,
  expiryTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  payloads varbinary(10240),
  PRIMARY KEY (surrogateId),
  INDEX saga_events_index (surrogateId, globalTxId, localTxId, type, expiryTime)
) DEFAULT CHARSET=utf8;


CREATE TABLE IF NOT EXISTS Command (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  eventId bigint NOT NULL UNIQUE,
  serviceName varchar(16) NOT NULL,
  instanceId varchar(36) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  compensationMethod varchar(256) NOT NULL,
  payloads varbinary(10240),
  status varchar(12),
  lastModified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version bigint NOT NULL,
  PRIMARY KEY (surrogateId),
  INDEX saga_commands_index (surrogateId, eventId, globalTxId, localTxId, status)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS TxTimeout (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  eventId bigint NOT NULL UNIQUE,
  serviceName varchar(16) NOT NULL,
  instanceId varchar(36) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  type varchar(50) NOT NULL,
  expiryTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status varchar(12),
  version bigint NOT NULL,
  PRIMARY KEY (surrogateId),
  INDEX saga_timeouts_index (surrogateId, expiryTime, globalTxId, localTxId, status)
) DEFAULT CHARSET=utf8;