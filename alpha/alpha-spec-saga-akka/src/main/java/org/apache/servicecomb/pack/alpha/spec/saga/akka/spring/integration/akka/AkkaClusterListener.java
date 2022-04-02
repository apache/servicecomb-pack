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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AkkaClusterListener extends AbstractActor {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  LoggingAdapter AKKA_LOG = Logging.getLogger(getContext().getSystem(), this);
  Cluster cluster = Cluster.get(getContext().getSystem());

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(MemberUp.class, mUp -> {
          LOG.info("Member is Up: {}", mUp.member());
        })
        .match(UnreachableMember.class, mUnreachable -> {
          LOG.info("Member detected as unreachable: {}", mUnreachable.member());
        })
        .match(MemberRemoved.class, mRemoved -> {
          LOG.info("Member is Removed: {}", mRemoved.member());
        })
        .match(MemberEvent.class, message -> {
          // ignore
        })
        .matchAny(msg -> AKKA_LOG.warning("Received unknown message: {}", msg))
        .build();
  }

  //subscribe to cluster changes
  @Override
  public void preStart() {
    cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
        MemberEvent.class, UnreachableMember.class);
  }

  //re-subscribe when restart
  @Override
  public void postStop() {
    cluster.unsubscribe(getSelf());
  }

  @Override
  public void preRestart(Throwable reason, Optional<Object> message) {
    AKKA_LOG.error(
        reason,
        "Restarting due to [{}] when processing [{}]",
        reason.getMessage(),
        message.isPresent() ? message.get() : "");
  }
}
