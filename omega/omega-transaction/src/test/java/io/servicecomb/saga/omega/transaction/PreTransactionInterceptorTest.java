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

package io.servicecomb.saga.omega.transaction;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class PreTransactionInterceptorTest {
  private final List<byte[]> messages = new ArrayList<>();
  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();

  private final MessageSender sender = (msg) -> messages.add(
      serialize(msg.globalTxId(),
          msg.localTxId(),
          msg.parentTxId(),
          (String) msg.payloads()[0]));

  private final String message = uniquify("message");
  private final PreTransactionInterceptor interceptor = new PreTransactionInterceptor(sender);

  private byte[] serialize(String globalTxId, String localTxId, String parentTxId, String message) {
    return (globalTxId + ":" + localTxId + ":" + parentTxId + ":" + message).getBytes();
  }

  @Test
  public void sendsSerializedMessage() throws Exception {
    interceptor.intercept(globalTxId, localTxId, parentTxId, message);

    assertThat(messages, contains(serialize(globalTxId, localTxId, parentTxId, message)));
  }
}
