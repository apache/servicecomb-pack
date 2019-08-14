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

import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.sink.ActorEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

public class KafkaMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageListener.class);

    private ActorEventSink actorEventSink;

    public KafkaMessageListener(ActorEventSink actorEventSink) {
        this.actorEventSink = actorEventSink;
    }

    @KafkaListener(topics = "${alpha.feature.akka.channel.kafka.topic:servicecomb-pack-actor-event}")
    public void listener(BaseEvent baseEvent, Acknowledgment acknowledgment){
        if(logger.isDebugEnabled()){
            logger.debug("listener event = [{}]", baseEvent);
        }

        try {
            actorEventSink.send(baseEvent);
            acknowledgment.acknowledge();
        }catch (Exception e){
            logger.error("subscriber Exception = [{}]", e.getMessage(), e);
        }
    }
}