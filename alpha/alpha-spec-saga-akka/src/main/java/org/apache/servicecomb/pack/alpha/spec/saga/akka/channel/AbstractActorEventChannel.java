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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel;

import org.apache.servicecomb.pack.alpha.core.fsm.channel.ActorEventChannel;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractActorEventChannel implements ActorEventChannel {
  private static final Logger logger = LoggerFactory.getLogger(AbstractActorEventChannel.class);

  protected final MetricsService metricsService;

  public abstract void sendTo(BaseEvent event);

  public AbstractActorEventChannel(
      MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  public void send(BaseEvent event) {
    long begin = System.currentTimeMillis();
    metricsService.metrics().doEventReceived();
    try {
      this.sendTo(event);
      metricsService.metrics().doEventAccepted();
    } catch (Exception ex) {
      logger.error("send Exception = [{}]", ex.getMessage(), ex);
      metricsService.metrics().doEventRejected();
    }
    long end = System.currentTimeMillis();
    metricsService.metrics().doEventAvgTime(end - begin);
  }

}
