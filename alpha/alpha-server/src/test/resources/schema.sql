CREATE TABLE IF NOT EXISTS `TxEvent` (
  `surrogateId` bigint NOT NULL AUTO_INCREMENT,
  `serviceName` varchar(36) NOT NULL,
  `instanceId` varchar(36) NOT NULL,
  `creationTime` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `globalTxId` varchar(36) NOT NULL,
  `localTxId` varchar(36) NOT NULL,
  `parentTxId` varchar(36) DEFAULT NULL,
  `type` varchar(50) NOT NULL,
  `compensationMethod` varchar(256) NOT NULL,
  `payloads` varbinary(10240),
  PRIMARY KEY (`surrogateId`)
) DEFAULT CHARSET=utf8;
