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
package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.sink.ActorEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

public class RedisMessageSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private ActorEventSink actorEventSink;
    private NodeStatus nodeStatus;

    private MessageSerializer messageSerializer = new MessageSerializer();

    public RedisMessageSubscriber(ActorEventSink actorEventSink, NodeStatus nodeStatus) {
        this.actorEventSink = actorEventSink;
        this.nodeStatus = nodeStatus;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if(nodeStatus.isMaster()) {
            if (logger.isDebugEnabled()) {
                logger.debug("pattern = [{}]", new String(pattern, StandardCharsets.UTF_8));
            }

            messageSerializer.deserialize(message.getBody()).ifPresent(data -> {

                BaseEvent event = (BaseEvent) data;

                if (logger.isDebugEnabled()) {
                    logger.debug("event = [{}]", event);
                }

                try {
                    actorEventSink.send(event);
                } catch (Exception e) {
                    logger.error("subscriber Exception = [{}]", e.getMessage(), e);
                }
            });
        }else{
            if(logger.isDebugEnabled()){
                logger.debug("nodeStatus is not master and cancel this time subscribe");
            }
        }
    }
}
