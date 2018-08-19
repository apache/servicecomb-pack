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

package org.apache.servicecomb.saga.omega.connector.grpc;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class MessageSenderManager {


  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final long DEFAULT_TIMEOUT_MILLIS = 1000;

  /**
   * Timeout in mills
   */
  private final long timeout;

  private final Map<MessageSenderWithWeight, Integer> objectIndex;

  private final PriorityBlockingQueue<MessageSenderWithWeight> messageSenders;

  private final ScheduledExecutorService scheduledExecutorService;

  public MessageSenderManager(
      Collection<MessageSender> messageSenders) {
    this(DEFAULT_TIMEOUT_MILLIS, messageSenders);
  }

  /**
   * @param timeout Timeout in millis
   * @param messageSenders messageSenders which initializes the messageSenderManager
   */
  public MessageSenderManager(long timeout, Collection<? extends MessageSender> messageSenders) {
    this.timeout = timeout;
    this.messageSenders = createMessageSenderQueue(messageSenders);
    this.objectIndex = createObjectIndex(this.messageSenders);
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder()
            .setNameFormat("MessageSenderManager-Recycle-Pool")
            .build()
    );
  }

  public <T> T use(UsingMessageSenderCallback<T> messageSenderCallback) {
    MessageSenderWithWeight messageSender = null;
    try {
      messageSender = messageSenders.poll(timeout, TimeUnit.MILLISECONDS);
      if (messageSender == null) {
        throw new OmegaException("Timeout trying to get connection");
      }

      MessageSenderUsingLifeCycleManager lifeCycleManager = lifeCycleManagerFactory()
          .manages(messageSender);
      lifeCycleManager.beforeUsing();
      T returnValue = messageSenderCallback.using(messageSender);
      lifeCycleManager.afterUsing();

      return returnValue;
    } catch (InterruptedException e) {
      throw new OmegaException("Interrupted at borrowing MessageSender", e);
    } catch (Exception e) {
      throw new OmegaException("Unkonwn exception at borrowing MessageSender", e);
    } finally {
      if (messageSender != null) {
        returnObject(messageSender);
      }
    }
  }

  public void forAllDo(UsingMessageSenderCallback<Void> messageSenderCallback) {
    synchronized (messageSenders) {
      for (MessageSender messageSender : messageSenders) {
        try {
          messageSenderCallback.using(messageSender);
        } catch (Exception e) {
          LOG.error("Failed operating on messageSender {} : {}", messageSender.target(), e);
        }
      }
    }
  }

  abstract MessageSenderUsingLifeCycleManagerFactory lifeCycleManagerFactory();

  private void returnObject(final MessageSenderWithWeight messageSender) {
    scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        if (objectIndex.containsKey(messageSender)) {
          messageSenders.add(messageSender);
        }
      }
    }, 0, 5, TimeUnit.MILLISECONDS);
  }


  private PriorityBlockingQueue<MessageSenderWithWeight> createMessageSenderQueue(
      Collection<? extends MessageSender> messageSenders
  ) {
    PriorityBlockingQueue<MessageSenderWithWeight> queue = new PriorityBlockingQueue<>(
        messageSenders.size(), new Comparator<MessageSenderWithWeight>() {
      @Override
      public int compare(MessageSenderWithWeight o1, MessageSenderWithWeight o2) {
        return Long.compare(o1.getWeight(), o2.getWeight());
      }
    });

    for (MessageSender messageSender : messageSenders) {
      queue.add(new MessageSenderWithWeight(messageSender, 0L));
    }
    return queue;
  }

  private ImmutableMap<MessageSenderWithWeight, Integer> createObjectIndex(
      Collection<? extends MessageSender> messageSenders
  ) {
    ImmutableMap.Builder<MessageSenderWithWeight, Integer> map = ImmutableMap
        .builder();
    for (MessageSender messageSender : messageSenders) {
      if (messageSender instanceof MessageSenderWithWeight) {
        map.put((MessageSenderWithWeight) messageSender, 1);
      }
    }
    return map.build();
  }

}

class MessageSenderWithWeight implements MessageSender {

  private MessageSender instance;


  private long weight;

  MessageSenderWithWeight(MessageSender instance, long weight) {
    this.instance = instance;
    this.weight = weight;
  }

  @Override
  public void onConnected() {
    instance.onConnected();
  }

  @Override
  public void onDisconnected() {
    instance.onDisconnected();
  }

  @Override
  public void close() {
    instance.close();
  }

  @Override
  public String target() {
    return instance.target();
  }

  @Override
  public AlphaResponse send(
      TxEvent event) {
    return instance.send(event);
  }

  synchronized long getWeight() {
    return weight;
  }

  synchronized void setWeight(long weight) {
    this.weight = weight;
  }
}
