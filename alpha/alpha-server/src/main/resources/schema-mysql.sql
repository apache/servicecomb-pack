/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE IF NOT EXISTS TxEvent (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  serviceName varchar(36) NOT NULL,
  instanceId varchar(36) NOT NULL,
  creationTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  type varchar(50) NOT NULL,
  compensationMethod varchar(256) NOT NULL,
  expiryTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  payloads blob,
  retries int(11) NOT NULL DEFAULT '0',
  retryMethod varchar(256) DEFAULT NULL,
  PRIMARY KEY (surrogateId),
  INDEX saga_events_index (surrogateId, globalTxId, localTxId, type, expiryTime),
  INDEX saga_global_tx_index (globalTxId)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS Command (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  eventId bigint NOT NULL UNIQUE,
  serviceName varchar(36) NOT NULL,
  instanceId varchar(36) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  compensationMethod varchar(256) NOT NULL,
  payloads blob,
  status varchar(12),
  lastModified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version bigint NOT NULL,
  PRIMARY KEY (surrogateId),
  INDEX saga_commands_index (surrogateId, eventId, globalTxId, localTxId, status)
) DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS TxTimeout (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  eventId bigint NOT NULL UNIQUE,
  serviceName varchar(36) NOT NULL,
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

CREATE TABLE IF NOT EXISTS TxEventHistory AS SELECT * FROM TxEvent WHERE 1=2;



CREATE TABLE IF NOT EXISTS tcc_global_tx_event (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  serviceName varchar(36) NOT NULL,
  instanceId varchar(36) NOT NULL,
  txType varchar(12),
  status varchar(12),
  creationTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastModified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (surrogateId),
  UNIQUE INDEX tcc_global_tx_event_index (globalTxId, localTxId, parentTxId, txType)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS tcc_participate_event (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  serviceName varchar(36) NOT NULL,
  instanceId varchar(36) NOT NULL,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  confirmMethod varchar(256) NOT NULL,
  cancelMethod varchar(256) NOT NULL,
  status varchar(50) NOT NULL,
  creationTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastModified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (surrogateId),
  UNIQUE INDEX tcc_participate_event_index (globalTxId, localTxId, parentTxId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS tcc_tx_event (
  surrogateId bigint NOT NULL AUTO_INCREMENT,
  globalTxId varchar(36) NOT NULL,
  localTxId varchar(36) NOT NULL,
  parentTxId varchar(36) DEFAULT NULL,
  serviceName varchar(36) NOT NULL,
  instanceId varchar(36) NOT NULL,
  methodInfo varchar(512) NOT NULL,
  txType varchar(12),
  status varchar(12),
  creationTime datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastModified datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (surrogateId),
  UNIQUE INDEX tcc_tx_event_index (globalTxId, localTxId, parentTxId, txType)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
