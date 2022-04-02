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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.AbstractEventConsumer;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemorySagaEventConsumer extends AbstractEventConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  final MemoryActorEventChannel channel;

  public MemorySagaEventConsumer(ActorSystem actorSystem, ActorRef sagaShardRegionActor, MetricsService metricsService,
      MemoryActorEventChannel channel) {
    super(actorSystem, sagaShardRegionActor, metricsService);
    this.channel = channel;
    new Thread(new MemorySagaEventConsumer.EventConsumer(), "MemorySagaEventConsumer").start();
  }

  class EventConsumer implements Runnable {

    @Override
    public void run() {
      while (true) {
        try {
          BaseEvent event = channel.getEventQueue().peek();
          if (event != null) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("event {}", event);
            }
            long begin = System.currentTimeMillis();
            metricsService.metrics().doActorReceived();
            sagaShardRegionActor.tell(event, sagaShardRegionActor);
            long end = System.currentTimeMillis();
            metricsService.metrics().doActorAccepted();
            metricsService.metrics().doActorAvgTime(end - begin);
            channel.getEventQueue().poll();
          } else {
            Thread.sleep(10);
          }
        } catch (Exception ex) {
          metricsService.metrics().doActorRejected();
          LOG.error(ex.getMessage(), ex);
        }
      }
    }
  }
}
