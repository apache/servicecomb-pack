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
package org.apache.servicecomb.pack.alpha.fsm.channel.kafka;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.sink.ActorEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

public class KafkaMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageListener.class);

    private ActorEventSink actorEventSink;
    private NodeStatus nodeStatus;

    public KafkaMessageListener(ActorEventSink actorEventSink, NodeStatus nodeStatus) {
        this.actorEventSink = actorEventSink;
        this.nodeStatus = nodeStatus;
    }

    @KafkaListener(id = "messageListener", topics = "${alpha.feature.akka.channel.kafka.topic:servicecomb-pack-actor-event}")
    public void listener(BaseEvent baseEvent){
        if(logger.isDebugEnabled()){
            logger.debug("listener event = [{}]", baseEvent);
        }

        if(nodeStatus.isMaster()){
            try {
                actorEventSink.send(baseEvent);
            }catch (Exception e){
                logger.error("subscriber Exception = [{}]", e.getMessage(), e);
            }
        }else{
            if(logger.isDebugEnabled()){
                logger.debug("nodeStatus is not master and cancel this time subscribe");
            }
        }

    }
}