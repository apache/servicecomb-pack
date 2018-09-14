/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server.tcc.jpa;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "tcc_participate_event_history")
public class ParticipatedEventHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long surrogateId;
  private String globalTxId;
  private String localTxId;
  private String parentTxId;
  private String serviceName;
  private String instanceId;
  private String confirmMethod;
  private String cancelMethod;
  private String status;
  private Date creationTime;
  private Date lastModified;

  private ParticipatedEventHistory() {
  }

  public ParticipatedEventHistory(String globalTxId, String localTxId, String parentTxId, String serviceName,
      String instanceId, String confirmMethod, String cancelMethod, String status) {
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.serviceName = serviceName;
    this.instanceId = instanceId;
    this.confirmMethod = confirmMethod;
    this.cancelMethod = cancelMethod;
    this.status = status;
    this.creationTime = new Date();
    this.lastModified = new Date();
  }
}
