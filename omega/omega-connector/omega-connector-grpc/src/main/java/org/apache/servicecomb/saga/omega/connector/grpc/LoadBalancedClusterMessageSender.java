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

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.servicecomb.saga.omega.transaction.AlphaResponse;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;

public class LoadBalancedClusterMessageSender implements MessageSender {

  private final MessageSenderManager messageSenderManager;

  public LoadBalancedClusterMessageSender(final MessageSender... messageSenders) {
    this(null, new ClusterMessageSenderFactory() {
      @Nullable
      @Override
      public List<MessageSender> apply(@Nullable AlphaClusterConfig input) {
        return Lists.newArrayList(messageSenders);
      }
    }, FastestMessageSenderManager.FACTORY);
  }

  public LoadBalancedClusterMessageSender(AlphaClusterConfig clusterConfig,
      ClusterMessageSenderFactory clusterMessageSenderFactory,
      MessageSenderManagerFactory messageSenderManagerFactory) {

    List<MessageSender> messageSenders = Optional.fromNullable(clusterMessageSenderFactory
        .apply(clusterConfig))
        .or(Collections.<MessageSender>emptyList());

    this.messageSenderManager = messageSenderManagerFactory.newMessageSenderManager(messageSenders);
  }

  @Override
  public void onConnected() {
    messageSenderManager.forAllDo(new UsingMessageSenderCallback<Void>() {
      @Override
      public Void using(MessageSender messageSender) {
        messageSender.onConnected();
        return null;
      }
    });
  }

  @Override
  public void onDisconnected() {
    messageSenderManager.forAllDo(new UsingMessageSenderCallback<Void>() {
      @Override
      public Void using(MessageSender messageSender) {
        messageSender.onDisconnected();
        return null;
      }
    });
  }

  @Override
  public void close() {
    messageSenderManager.forAllDo(new UsingMessageSenderCallback<Void>() {
      @Override
      public Void using(MessageSender messageSender) {
        messageSender.close();
        return null;
      }
    });
  }

  @Override
  public String target() {
    return "UNKNOWN";
  }

  @Override
  public AlphaResponse send(final TxEvent event) {
    return messageSenderManager.use(
        new UsingMessageSenderCallback<AlphaResponse>() {
          @Override
          public AlphaResponse using(MessageSender messageSender) {
            return messageSender.send(event);
          }
        });
  }
}
