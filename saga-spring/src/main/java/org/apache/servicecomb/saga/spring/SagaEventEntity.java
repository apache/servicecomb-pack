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

package org.apache.servicecomb.saga.spring;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class SagaEventEntity {
  @Id
  @GeneratedValue
  private long id;
  private String sagaId;
  private Date creationTime;
  private String type;
  private String contentJson;

  public SagaEventEntity() {
  }

  public SagaEventEntity(String sagaId, String type, String contentJson) {
    this.sagaId = sagaId;
    this.type = type;
    this.contentJson = contentJson;
    this.creationTime = new Date();
  }

  public SagaEventEntity(long id, String sagaId, long timestamp, String type, String contentJson) {
    this.id = id;
    this.sagaId = sagaId;
    this.creationTime = new Date(timestamp);
    this.type = type;
    this.contentJson = contentJson;
  }

  public long id() {
    return id;
  }

  public String sagaId() {
    return sagaId;
  }

  public long creationTime() {
    return creationTime.getTime();
  }

  public String type() {
    return type;
  }

  public String contentJson() {
    return contentJson;
  }

  @Override
  public String toString() {
    return "SagaEventEntity{" +
        "id=" + id +
        ", sagaId='" + sagaId + '\'' +
        ", creationTime=" + creationTime +
        ", type='" + type + '\'' +
        ", contentJson='" + contentJson + '\'' +
        '}';
  }
}
