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

package io.servicecomb.saga.omega.connector.thrift;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import io.servicecomb.saga.omega.transaction.MessageSerializer;
import io.servicecomb.saga.omega.transaction.TxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEventEndpoint;

public class ThriftMessageSenderTest {

  private final String globalTxId = uniquify("global tx id");
  private final String localTxId = uniquify("local tx id");
  private final String parentTxId = uniquify("parent tx id");
  private final String payload1 = uniquify("payload1");
  private final String payload2 = uniquify("payload2");

  private SwiftTxEvent swiftTxEvent;

  private final MessageSerializer serializer = (event) -> {
    try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
      for (Object o : event.payloads()) {
        stream.write(o.toString().getBytes());
      }
      return stream.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  };

  private final SwiftTxEventEndpoint eventService = new SwiftTxEventEndpoint() {
    @Override
    public void handle(SwiftTxEvent message) {
      swiftTxEvent = message;
    }

    @Override
    public void close() throws Exception {
    }
  };

  private final ThriftMessageSender messageSender = new ThriftMessageSender(eventService, serializer);

  @Test
  public void sendSerializedEvent() throws Exception {
    TxEvent event = new TxEvent(globalTxId, localTxId, parentTxId, payload1, payload2);

    messageSender.send(event);

    assertThat(swiftTxEvent.globalTxId(), is(event.globalTxId()));
    assertThat(swiftTxEvent.localTxId(), is(event.localTxId()));
    assertThat(swiftTxEvent.parentTxId(), is(event.parentTxId()));
    assertThat(swiftTxEvent.payloads(), is(serializer.serialize(event)));
  }
}
