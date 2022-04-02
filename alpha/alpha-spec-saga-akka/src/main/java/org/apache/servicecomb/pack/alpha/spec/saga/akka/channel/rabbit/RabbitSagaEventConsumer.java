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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.rabbit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.AbstractEventConsumer;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class RabbitSagaEventConsumer extends AbstractEventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public RabbitSagaEventConsumer(ActorSystem actorSystem, ActorRef sagaShardRegionActor,
                                   MetricsService metricsService) {
        super(actorSystem, sagaShardRegionActor, metricsService);

    }

    @StreamListener(RabbitMessageChannel.SERVICE_COMB_PACK_CONSUMER)
    public void receive(BaseEvent baseEvent) {
        sendSagaActor(baseEvent);
    }


    private CompletionStage<String> sendSagaActor(BaseEvent event) {
        try {
            long begin = System.currentTimeMillis();
            metricsService.metrics().doActorReceived();
            Timeout timeout = new Timeout(Duration.create(10, "seconds"));
            Future<Object> future = Patterns.ask(sagaShardRegionActor, event, timeout);
            Await.result(future, timeout.duration());
            long end = System.currentTimeMillis();
            metricsService.metrics().doActorAccepted();
            metricsService.metrics().doActorAvgTime(end - begin);
            return CompletableFuture.completedFuture("OK");
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            metricsService.metrics().doActorRejected();
            throw new CompletionException(ex);
        }
    }
}
