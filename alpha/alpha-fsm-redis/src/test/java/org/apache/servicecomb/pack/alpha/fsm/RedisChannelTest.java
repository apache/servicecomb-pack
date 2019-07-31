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
package org.apache.servicecomb.pack.alpha.fsm;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.channel.redis.MessageSerializer;
import org.apache.servicecomb.pack.alpha.fsm.channel.redis.RedisMessagePublisher;
import org.apache.servicecomb.pack.alpha.fsm.channel.redis.RedisMessageSubscriber;
import org.apache.servicecomb.pack.alpha.core.fsm.sink.ActorEventSink;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class RedisChannelTest {

    @Mock
    private RedisConnection redisConnection;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisConnectionFactory redisConnectionFactory;

    @Spy
    private ChannelTopic channelTopic = new ChannelTopic("redis-channel");

    private RedisMessageListenerContainer redisMessageListenerContainer;

    @Spy
    private NodeStatus nodeStatus = new NodeStatus(NodeStatus.TypeEnum.MASTER);

    @Spy
    private ActorEventSink actorEventSink = new RedisEventSink();

    private RedisMessagePublisher redisMessagePublisher;

    private RedisMessageSubscriber redisMessageSubscriber;

    private MessageListenerAdapter messageListenerAdapter;

    @Before
    public void setup(){
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);

        redisTemplate.afterPropertiesSet();

        redisMessageSubscriber = new RedisMessageSubscriber(actorEventSink, nodeStatus);
        messageListenerAdapter = new MessageListenerAdapter(redisMessageSubscriber);
        messageListenerAdapter.afterPropertiesSet();

        redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        redisMessageListenerContainer.addMessageListener(messageListenerAdapter, channelTopic);
        redisMessageListenerContainer.afterPropertiesSet();
        redisMessageListenerContainer.start();

        redisMessagePublisher = new RedisMessagePublisher(redisTemplate, channelTopic);

    }


    @Test
    public void testRedisPubSub(){
        final String globalTxId = UUID.randomUUID().toString().replaceAll("-", "");
        final String localTxId1 = UUID.randomUUID().toString().replaceAll("-", "");
        final String localTxId2 = UUID.randomUUID().toString().replaceAll("-", "");
        final String localTxId3 = UUID.randomUUID().toString().replaceAll("-", "");

        MessageSerializer messageSerializer = new MessageSerializer();
        buildData(globalTxId, localTxId1, localTxId2, localTxId3).forEach(baseEvent -> {
            redisMessagePublisher.publish(baseEvent);
            redisMessageSubscriber.onMessage(new DefaultMessage(channelTopic.getTopic().getBytes(), messageSerializer.serializer(baseEvent).orElse(new byte[0])), channelTopic.getTopic().getBytes());
        });
    }

    private List<BaseEvent> buildData(String globalTxId, String localTxId_1, String localTxId_2, String localTxId_3){
        List<BaseEvent> sagaEvents = new ArrayList<>();
        sagaEvents.add(SagaStartedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
        sagaEvents.add(TxStartedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
        sagaEvents.add(TxEndedEvent.builder().serviceName("service_c1").instanceId("instance_c1").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_1).build());
        sagaEvents.add(TxStartedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
        sagaEvents.add(TxEndedEvent.builder().serviceName("service_c2").instanceId("instance_c2").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_2).build());
        sagaEvents.add(TxStartedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
        sagaEvents.add(TxEndedEvent.builder().serviceName("service_c3").instanceId("instance_c3").globalTxId(globalTxId).parentTxId(globalTxId).localTxId(localTxId_3).build());
        sagaEvents.add(SagaEndedEvent.builder().serviceName("service_g").instanceId("instance_g").globalTxId(globalTxId).build());
        return sagaEvents;
    }
}


