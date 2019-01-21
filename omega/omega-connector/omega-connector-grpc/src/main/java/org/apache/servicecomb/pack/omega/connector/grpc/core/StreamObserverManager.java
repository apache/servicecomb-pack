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

package org.apache.servicecomb.pack.omega.connector.grpc.core;

import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import org.apache.servicecomb.pack.omega.transaction.MessageSender;

public class StreamObserverManager {

  private static final Map<StreamObserver, MessageSender> SENDER_OBSERVER_MAP = new HashMap<>();

  public static void register(final StreamObserver streamObserver, final MessageSender messageSender) {
    SENDER_OBSERVER_MAP.put(streamObserver, messageSender);
  }

  public static MessageSender getMessageSender(final StreamObserver streamObserver) {
    return SENDER_OBSERVER_MAP.get(streamObserver);
  }
}
