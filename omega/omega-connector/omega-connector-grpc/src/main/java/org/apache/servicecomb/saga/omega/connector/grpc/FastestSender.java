/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import com.google.common.base.Supplier;
import java.util.Map;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;

/**
 * The strategy of picking the fastest {@link MessageSender}
 */
public class FastestSender implements MessageSenderPicker {

  @Override
  public MessageSender pick(Map<MessageSender, Long> messageSenders,
      Supplier<MessageSender> defaultSender) {
    Long min = Long.MAX_VALUE;
    MessageSender sender = null;
    for (Map.Entry<MessageSender, Long> entry : messageSenders.entrySet()) {
      if (entry.getValue() != Long.MAX_VALUE && min > entry.getValue()) {
        min = entry.getValue();
        sender = entry.getKey();
      }
    }
    if (sender == null) {
      return defaultSender.get();
    } else {
      return sender;
    }
  }
}
