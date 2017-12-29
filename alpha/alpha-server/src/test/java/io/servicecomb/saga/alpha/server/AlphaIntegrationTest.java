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
import static io.servicecomb.saga.alpha.core.EventType.TxAbortedEvent;
import static io.servicecomb.saga.alpha.core.EventType.TxEndedEvent;
import static io.servicecomb.saga.alpha.core.EventType.TxStartedEvent;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.swift.service.ThriftClientManager;

import io.servicecomb.saga.alpha.core.EventType;
import io.servicecomb.saga.alpha.core.OmegaCallback;
import io.servicecomb.saga.alpha.core.TxEvent;
import io.servicecomb.saga.alpha.server.AlphaIntegrationTest.OmegaCallbackConfig;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEvent;
import io.servicecomb.saga.pack.contracts.thrift.SwiftTxEventEndpoint;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class, OmegaCallbackConfig.class}, properties = "alpha.server.port=8090")
public class AlphaIntegrationTest {
  private static final ThriftClientManager clientManager = new ThriftClientManager();
  private static final String payload = "hello world";

  private final int port = 8090;

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String compensationMethod = getClass().getCanonicalName();

  @Autowired
  private TxEventEnvelopeRepository eventRepo;

  @Autowired
  private List<CompensationContext> compensationContexts;

  private final FramedClientConnector connector = new FramedClientConnector(fromParts("localhost", port));
  private SwiftTxEventEndpoint endpoint;

  @AfterClass
  public static void tearDown() throws Exception {
    clientManager.close();
  }

  @Before
  public void setUp() throws Exception {
    endpoint = clientManager.createClient(connector, SwiftTxEventEndpoint.class).get();
  }

  @After
  public void after() throws Exception {
    endpoint.close();
  }

  @Test
  public void persistsEvent() throws Exception {
    endpoint.handle(someEvent(TxStartedEvent));

    TxEventEnvelope envelope = eventRepo.findByEventGlobalTxId(globalTxId);

    assertThat(envelope.globalTxId(), is(globalTxId));
    assertThat(envelope.localTxId(), is(localTxId));
    assertThat(envelope.parentTxId(), is(parentTxId));
    assertThat(envelope.type(), is(TxStartedEvent.name()));
    assertThat(envelope.compensationMethod(), is(compensationMethod));
    assertThat(envelope.payloads(), is(payload.getBytes()));
  }

  @Test
  public void doNotCompensateDuplicateTxOnFailure() throws Exception {
    // duplicate events with same content but different timestamp
    eventRepo.save(eventEnvelopeOf(TxStartedEvent, localTxId, parentTxId, "service a".getBytes()));
    eventRepo.save(eventEnvelopeOf(TxStartedEvent, localTxId, parentTxId, "service a".getBytes()));
    eventRepo.save(eventEnvelopeOf(TxEndedEvent, new byte[0]));

    String localTxId1 = UUID.randomUUID().toString();
    eventRepo.save(eventEnvelopeOf(TxStartedEvent, localTxId1, UUID.randomUUID().toString(), "service b".getBytes()));
    eventRepo.save(eventEnvelopeOf(TxEndedEvent, new byte[0]));

    endpoint.handle(someEvent(TxAbortedEvent));

    await().atMost(1, SECONDS).until(() -> compensationContexts.size() > 1);
    assertThat(compensationContexts, containsInAnyOrder(
        new CompensationContext(globalTxId, this.localTxId, compensationMethod, "service a".getBytes()),
        new CompensationContext(globalTxId, localTxId1, compensationMethod, "service b".getBytes())
    ));
  }

  private SwiftTxEvent someEvent(EventType type) {
    return new SwiftTxEvent(
        System.currentTimeMillis(),
        this.globalTxId,
        this.localTxId,
        this.parentTxId,
        type.name(),
        compensationMethod,
        payload.getBytes());
  }

  private TxEventEnvelope eventEnvelopeOf(EventType eventType, byte[] payloads) {
    return eventEnvelopeOf(eventType, UUID.randomUUID().toString(), UUID.randomUUID().toString(), payloads);
  }

  private TxEventEnvelope eventEnvelopeOf(EventType eventType, String localTxId, String parentTxId, byte[] payloads) {
    return new TxEventEnvelope(new TxEvent(new Date(),
        globalTxId,
        localTxId,
        parentTxId,
        eventType.name(),
        compensationMethod,
        payloads));
  }

  @Configuration
  static class OmegaCallbackConfig {
    private final List<CompensationContext> compensationContexts = new ArrayList<>();

    @Bean
    List<CompensationContext> compensationContexts() {
      return compensationContexts;
    }

    @Bean
    OmegaCallback omegaCallback() {
      return (globalTxId, localTxId, compensationMethod, payloads) ->
          compensationContexts.add(new CompensationContext(globalTxId, localTxId, compensationMethod, payloads));
    }
  }

  private static class CompensationContext {
    private final String globalTxId;
    private final String localTxId;
    private final String compensationMethod;
    private final byte[] message;

    private CompensationContext(String globalTxId, String localTxId, String compensationMethod, byte[] message) {
      this.globalTxId = globalTxId;
      this.localTxId = localTxId;
      this.compensationMethod = compensationMethod;
      this.message = message;
    }

    @Override
    public String toString() {
      return "CompensationContext{" +
          "globalTxId='" + globalTxId + '\'' +
          ", localTxId='" + localTxId + '\'' +
          ", compensationMethod='" + compensationMethod + '\'' +
          ", message=" + Arrays.toString(message) +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CompensationContext that = (CompensationContext) o;
      return Objects.equals(globalTxId, that.globalTxId) &&
          Objects.equals(localTxId, that.localTxId) &&
          Objects.equals(compensationMethod, that.compensationMethod) &&
          Arrays.equals(message, that.message);
    }

    @Override
    public int hashCode() {
      return Objects.hash(globalTxId, localTxId, compensationMethod, message);
    }
  }
}
