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

package org.apache.servicecomb.pack.alpha.fsm.event.consumer;

import akka.actor.ActorNotFound;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.pack.alpha.fsm.SagaActor;
import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class SagaEventConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final Timeout TIMEOUT = new Timeout(5, TimeUnit.SECONDS);

  @Autowired
  ActorSystem system;

  /**
   * Receive fsm message
   * */
  @Subscribe
  public void receiveSagaEvent(BaseEvent event) throws Exception {
    LOG.info("receive {} ", event.toString());
    try{
      ActorRef saga;
      String actorPath = "/user/" + event.getGlobalTxId();
      Optional<ActorRef> optional = this.getActorRefFromPath(actorPath);
      if (!optional.isPresent()) {
        saga = system.actorOf(SagaActor.props(event.getGlobalTxId()), event.getGlobalTxId());
      } else {
        saga = optional.get();
      }
      saga.tell(event, ActorRef.noSender());
      LOG.info("tell {} to {}", event.toString(),saga);
      //TODO WAL commit
    }catch (Exception ex){
      //TODO
      throw ex;
    }
  }

  public Optional<ActorRef> getActorRefFromPath(String path) throws Exception {
    try {
      ActorSelection selection = system.actorSelection(path);
      Future<ActorRef> future = selection.resolveOne(TIMEOUT);
      ActorRef ref = Await.result(future, TIMEOUT.duration());
      return Optional.of(ref);
    } catch (ActorNotFound e) {
      return Optional.absent();
    }
  }

}
