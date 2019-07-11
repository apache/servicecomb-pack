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

package org.apache.servicecomb.pack.alpha.fsm.sink;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.util.Timeout;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.pack.alpha.fsm.SagaActor;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class SagaActorEventSender implements ActorEventSink {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  ActorSystem system;

  private static final Timeout lookupTimeout = new Timeout(Duration.create(1, TimeUnit.SECONDS));

  public void send(BaseEvent event) {
    try{
      if (LOG.isDebugEnabled()) {
        LOG.debug("send {} ", event.toString());
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("send {} ", event.toString());
      }
      if (event instanceof SagaStartedEvent) {
        final ActorRef saga = system
            .actorOf(SagaActor.props(event.getGlobalTxId()), event.getGlobalTxId());
        saga.tell(event, ActorRef.noSender());
      } else {
        ActorSelection actorSelection = system
            .actorSelection("/user/" + event.getGlobalTxId());
        final Future<ActorRef> actorRefFuture = actorSelection.resolveOne(lookupTimeout);
        final ActorRef saga = Await.result(actorRefFuture, lookupTimeout.duration());
        saga.tell(event, ActorRef.noSender());
      }
    }catch (Exception ex){
      throw new RuntimeException(ex);
    }
  }
}
