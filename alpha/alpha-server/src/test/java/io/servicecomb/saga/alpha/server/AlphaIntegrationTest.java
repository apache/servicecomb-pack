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

package io.servicecomb.saga.alpha.server;

import static com.google.common.net.HostAndPort.fromParts;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.swift.service.ThriftClientManager;

import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEventEndpoint;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AlphaApplication.class, properties = "alpha.server.port=8090")
public class AlphaIntegrationTest {
  private static final ThriftClientManager clientManager = new ThriftClientManager();
  private static final String TX_STARTED_EVENT = "TxStartedEvent";
  private static final String payload = "hello world";

  private final int port = 8090;

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();

  @Autowired
  private TxEventEnvelopeRepository eventRepo;

  @AfterClass
  public static void tearDown() throws Exception {
    clientManager.close();
  }

  @Test
  public void persistsEvent() throws Exception {
    FramedClientConnector connector = new FramedClientConnector(fromParts("localhost", port));
    SwiftTxEventEndpoint endpoint = clientManager.createClient(connector, SwiftTxEventEndpoint.class).get();

    endpoint.handle(new SwiftTxEvent(
        System.currentTimeMillis(),
        globalTxId,
        localTxId,
        parentTxId,
        TX_STARTED_EVENT,
        payload.getBytes()));

    TxEventEnvelope envelope = eventRepo.findByEventGlobalTxId(globalTxId);

    assertThat(envelope.globalTxId(), is(globalTxId));
    assertThat(envelope.localTxId(), is(localTxId));
    assertThat(envelope.parentTxId(), is(parentTxId));
    assertThat(envelope.type(), is(TX_STARTED_EVENT));
    assertThat(envelope.payloads(), is(payload.getBytes()));
  }
}
