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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.eventbus.Subscribe;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import org.apache.servicecomb.pack.alpha.fsm.SagaActor;
import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SagaEventConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  ActorSystem system;

  private Map<String,ActorRef> sagaCache = new HashMap<>();

  /**
   * Receive fsm message
   * */
  @Subscribe
  public void receiveSagaEvent(BaseEvent event) {
    if(LOG.isDebugEnabled()){
      LOG.debug("receive {} ", event.toString());
    }
    try{
      ActorRef saga;
      if(sagaCache.containsKey(event.getGlobalTxId())){
        saga = sagaCache.get(event.getGlobalTxId());
      }else{
        saga = system.actorOf(SagaActor.props(event.getGlobalTxId()), event.getGlobalTxId());
        sagaCache.put(event.getGlobalTxId(), saga);
      }
      saga.tell(event, ActorRef.noSender());
      if(LOG.isDebugEnabled()){
        LOG.debug("tell {} to {}", event.toString(),saga);
      }
    }catch (Exception ex){
      throw ex;
    }
  }
}
