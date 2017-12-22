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

import org.junit.Test;

public class PreTransactionInterceptorTest {
  private final List<byte[]> messages = new ArrayList<>();

  private final MessageSender sender = messages::add;
  private final MessageSerializer serializer = messages -> {
    if (messages[0] instanceof String) {
      return ((String) messages[0]).getBytes();
    }
    throw new IllegalArgumentException("Expected instance of String, but was " + messages.getClass());
  };

  private final String message = uniquify("message");
  private final PreTransactionInterceptor interceptor = new PreTransactionInterceptor(sender, serializer);

  @Test
  public void sendsSerializedMessage() throws Exception {
    interceptor.intercept(message);

    assertThat(messages, contains(message.getBytes()));
  }
}
