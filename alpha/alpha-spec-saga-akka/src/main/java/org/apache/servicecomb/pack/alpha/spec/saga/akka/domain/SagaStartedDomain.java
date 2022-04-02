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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.domain;

import java.util.Calendar;
import java.util.Date;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;

public class SagaStartedDomain implements DomainEvent {

  private Date expirationTime;
  private BaseEvent event;

  public SagaStartedDomain(SagaStartedEvent event) {
    this.event = event;
    if (event.getTimeout() > 0) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(event.getCreateTime());
      calendar.add(Calendar.SECOND, event.getTimeout());
      this.expirationTime = calendar.getTime();
    }
  }

  public Date getExpirationTime() {
    return expirationTime;
  }

  @Override
  public BaseEvent getEvent() {
    return event;
  }
}
