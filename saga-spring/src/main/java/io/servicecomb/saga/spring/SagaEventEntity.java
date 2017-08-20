/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.spring;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class SagaEventEntity {
  @Id
  @GeneratedValue
  private long id;
  private String sagaId;
  private long timestamp;
  private String type;

  @Column(length = 1000)
  private String contentJson;

  public SagaEventEntity() {
  }

  public SagaEventEntity(String sagaId, String type, String contentJson) {
    this.sagaId = sagaId;
    this.type = type;
    this.contentJson = contentJson;
    this.timestamp = System.currentTimeMillis();
  }

  public long id() {
    return id;
  }

  public String type() {
    return type;
  }

  @Override
  public String toString() {
    return "SagaEventEntity{" +
        "id=" + id +
        ", sagaId='" + sagaId + '\'' +
        ", timestamp=" + timestamp +
        ", type='" + type + '\'' +
        ", contentJson='" + contentJson + '\'' +
        '}';
  }
}
