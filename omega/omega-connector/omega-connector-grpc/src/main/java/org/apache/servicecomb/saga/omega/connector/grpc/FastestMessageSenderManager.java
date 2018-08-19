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

import java.util.Collection;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;

public class FastestMessageSenderManager extends MessageSenderManager {


  public static final MessageSenderManagerFactory<FastestMessageSenderManager> FACTORY
      = new MessageSenderManagerFactory<FastestMessageSenderManager>() {
    @Override
    public FastestMessageSenderManager newMessageSenderManager(
        Collection<MessageSender> messageSenders) {
      return new FastestMessageSenderManager(messageSenders);
    }
  };

  public FastestMessageSenderManager(
      Collection<MessageSender> messageSenders) {
    super(messageSenders);
  }

  public FastestMessageSenderManager(long timeout,
      Collection<MessageSender> messageSenders) {
    super(timeout, messageSenders);
  }

  @Override
  MessageSenderUsingLifeCycleManagerFactory lifeCycleManagerFactory() {
    return new MessageSenderUsingLifeCycleManagerFactory() {
      @Override
      public MessageSenderUsingLifeCycleManager manages(final MessageSender messageSender) {
        return new MessageSenderUsingLifeCycleManager() {

          private long startTime;

          @Override
          public void beforeUsing() {
            startTime = System.nanoTime();
          }

          @Override
          public void afterUsing() {
            if (messageSender instanceof MessageSenderWithWeight) {
              MessageSenderWithWeight msw = (MessageSenderWithWeight) messageSender;
              long duration = System.nanoTime() - startTime;
              msw.setWeight(duration);
            }
          }
        };
      }
    };
  }
}
