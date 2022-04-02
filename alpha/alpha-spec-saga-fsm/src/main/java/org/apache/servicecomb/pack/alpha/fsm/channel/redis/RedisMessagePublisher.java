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


import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.MessagePublisher;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

public class RedisMessagePublisher implements MessagePublisher<BaseEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private RedisTemplate<String, Object> redisTemplate;
  private ChannelTopic channelTopic;

  public RedisMessagePublisher(RedisTemplate<String, Object> redisTemplate,
      ChannelTopic channelTopic) {
    this.redisTemplate = redisTemplate;
    this.channelTopic = channelTopic;
  }

  @Override
  public void publish(BaseEvent data) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("send message [{}] to [{}]", data, channelTopic.getTopic());
    }
    redisTemplate.convertAndSend(channelTopic.getTopic(), data);

  }
}
