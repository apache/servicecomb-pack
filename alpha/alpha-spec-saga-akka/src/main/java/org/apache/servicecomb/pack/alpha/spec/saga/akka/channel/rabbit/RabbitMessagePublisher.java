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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel.rabbit;

import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.fsm.channel.MessagePublisher;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;

public class RabbitMessagePublisher implements MessagePublisher<BaseEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private RabbitMessageChannel producerMessage;
    private int partitionCount;

    public RabbitMessagePublisher(int partitionCount, RabbitMessageChannel producerMessage) {

        this.partitionCount = partitionCount;
        this.producerMessage = producerMessage;

    }

    @Override
    public void publish(BaseEvent data) {

        String globalTxId = data.getGlobalTxId();
        int partitionIndex = (Math.abs(globalTxId.hashCode()))% partitionCount;
        if (LOG.isDebugEnabled()) {
            LOG.debug("send message [{}] to [{}]", data, partitionIndex);
        }
        // the headerName must consistent with partition key expression of spring cloud stream
        producerMessage.messageChannel().send(MessageBuilder.withPayload(data).setHeader("partitionKey", partitionIndex).build());

    }
}
