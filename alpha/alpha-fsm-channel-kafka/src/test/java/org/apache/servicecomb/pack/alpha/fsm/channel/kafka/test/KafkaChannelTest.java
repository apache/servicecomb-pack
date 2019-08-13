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
package org.apache.servicecomb.pack.alpha.fsm.channel.kafka.test;

import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.channel.kafka.KafkaMessagePublisher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.TimeUnit;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = KafkaApplication.class,
        properties = {
                "alpha.feature.akka.enabled=true",
                "alpha.feature.akka.channel.type=kafka",
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=messageListener"
        }
)
@EmbeddedKafka
public class KafkaChannelTest {
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaMessagePublisher kafkaMessagePublisher;

    @Before
    public void setup(){
    }
    @Test
    public void testProducer(){

        String globalTxId = UUID.randomUUID().toString().replaceAll("-", "");
        String localTxId_1 = UUID.randomUUID().toString().replaceAll("-", "");
        String localTxId_2 = UUID.randomUUID().toString().replaceAll("-", "");
        String localTxId_3 = UUID.randomUUID().toString().replaceAll("-", "");

        buildData(globalTxId, localTxId_1, localTxId_2, localTxId_3).forEach(baseEvent -> kafkaMessagePublisher.publish(baseEvent));

        try {
            TimeUnit.SECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
