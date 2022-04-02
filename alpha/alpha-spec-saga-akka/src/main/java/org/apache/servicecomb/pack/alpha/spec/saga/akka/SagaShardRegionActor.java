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

package org.apache.servicecomb.pack.alpha.spec.saga.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SagaShardRegionActor extends AbstractActor {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final ActorRef sagaActorRegion;

  static ShardRegion.MessageExtractor messageExtractor = new ShardRegion.MessageExtractor() {
    @Override
    public String entityId(Object message) {
      if (message instanceof BaseEvent) {
        return ((BaseEvent) message).getGlobalTxId();
      } else {
        return null;
      }
    }

    @Override
    public Object entityMessage(Object message) {
      return message;
    }

    @Override
    public String shardId(Object message) {
      int numberOfShards = 10; // NOTE: Greater than the number of alpha nodes
      if (message instanceof BaseEvent) {
        String actorId = ((BaseEvent) message).getGlobalTxId();
        return String.valueOf(actorId.hashCode() % numberOfShards);
      } else if (message instanceof ShardRegion.StartEntity) {
        String actorId = ((ShardRegion.StartEntity) message).entityId();
        return String.valueOf(actorId.hashCode() % numberOfShards);
      } else {
        return null;
      }
    }
  };

  public SagaShardRegionActor() {
    ActorSystem system = getContext().getSystem();
    ClusterShardingSettings settings = ClusterShardingSettings.create(system);
    sagaActorRegion = ClusterSharding.get(system)
        .start(
            SagaActor.class.getSimpleName(),
            SagaActor.props(null),
            settings,
            messageExtractor);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .matchAny(event -> {
          if(event instanceof BaseEvent){
            final BaseEvent evt = (BaseEvent) event;
            if (LOG.isDebugEnabled()) {
              LOG.debug("=> [{}] {} {}", evt.getGlobalTxId(), evt.getType(), evt.getLocalTxId());
            }

            sagaActorRegion.tell(event, getSelf());
            if (LOG.isDebugEnabled()) {
              LOG.debug("<= [{}] {} {}", evt.getGlobalTxId(), evt.getType(), evt.getLocalTxId());
            }
          }
          getSender().tell("confirm", getSelf());
        })
        .build();
  }
}
