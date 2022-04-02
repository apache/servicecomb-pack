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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.memory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.AbstractActorEventChannel;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryActorEventChannel extends AbstractActorEventChannel {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final LinkedBlockingQueue<BaseEvent> eventQueue;
  private int size;

  public MemoryActorEventChannel(MetricsService metricsService, int size) {
    super(metricsService);
    this.size = size > 0 ? size : Integer.MAX_VALUE;
    eventQueue = new LinkedBlockingQueue(this.size);
  }

  public LinkedBlockingQueue<BaseEvent> getEventQueue() {
    return eventQueue;
  }

  @Override
  public void sendTo(BaseEvent event) {
    try {
      eventQueue.put(event);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
