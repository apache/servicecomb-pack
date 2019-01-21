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

package org.apache.servicecomb.pack.omega.transaction;

import java.util.HashMap;
import java.util.Map;
import org.apache.servicecomb.pack.omega.context.TransactionType;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;

public class MessageHandlerManager {

  private static final Map<TransactionType, MessageHandler> MESSAGE_HANDLER_MAP = new HashMap<>();

  public static void register(final MessageHandler messageHandler) {
    if (messageHandler instanceof TccMessageHandler) {
      MESSAGE_HANDLER_MAP.put(TransactionType.TCC, messageHandler);
    } else if (messageHandler instanceof SagaMessageHandler) {
      MESSAGE_HANDLER_MAP.put(TransactionType.SAGA, messageHandler);
    } else {
      throw new UnsupportedOperationException("unsupported type of message handler.");
    }
  }

  public static SagaMessageHandler getSagaHandler() {
    MessageHandler result = MESSAGE_HANDLER_MAP.get(TransactionType.SAGA);
    return null == result ? null : (SagaMessageHandler) result;
  }

  public static TccMessageHandler getTccHandler() {
    MessageHandler result = MESSAGE_HANDLER_MAP.get(TransactionType.TCC);
    return null == result ? null : (TccMessageHandler) result;
  }
}
