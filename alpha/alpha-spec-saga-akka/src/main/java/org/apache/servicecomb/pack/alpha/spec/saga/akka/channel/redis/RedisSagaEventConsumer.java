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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.redis;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.AbstractEventConsumer;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class RedisSagaEventConsumer extends AbstractEventConsumer implements MessageListener {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private NodeStatus nodeStatus;
  private MessageSerializer messageSerializer = new MessageSerializer();

  public RedisSagaEventConsumer(ActorSystem actorSystem, ActorRef sagaShardRegionActor,
      MetricsService metricsService,
      NodeStatus nodeStatus) {
    super(actorSystem, sagaShardRegionActor, metricsService);
    this.nodeStatus = nodeStatus;
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    if (nodeStatus.isMaster()) {
      messageSerializer.deserialize(message.getBody()).ifPresent(data -> {
        BaseEvent event = (BaseEvent) data;
        if (LOG.isDebugEnabled()) {
          LOG.debug("event = [{}]", event);
        }
        try {
          long begin = System.currentTimeMillis();
          metricsService.metrics().doActorReceived();
          sagaShardRegionActor.tell(event, sagaShardRegionActor);
          long end = System.currentTimeMillis();
          metricsService.metrics().doActorAccepted();
          metricsService.metrics().doActorAvgTime(end - begin);
        } catch (Exception e) {
          metricsService.metrics().doActorRejected();
          LOG.error("subscriber Exception = [{}]", e.getMessage(), e);
        }
      });
    }
  }
}
