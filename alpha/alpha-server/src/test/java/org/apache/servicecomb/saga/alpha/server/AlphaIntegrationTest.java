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

package org.apache.servicecomb.saga.alpha.server;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.servicecomb.saga.alpha.core.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.alpha.core.EventType.TxStartedEvent;
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

import org.apache.servicecomb.saga.alpha.core.EventType;
import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.server.AlphaIntegrationTest.OmegaCallbackConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceStub;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaApplication.class, OmegaCallbackConfig.class}, properties = "alpha.server.port=8090")
public class AlphaIntegrationTest {
  private static final int port = 8090;

  private static ManagedChannel clientChannel = ManagedChannelBuilder
      .forAddress("localhost", port).usePlaintext(true).build();

  private TxEventServiceStub stub = TxEventServiceGrpc.newStub(clientChannel);

  private static final String payload = "hello world";

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String compensationMethod = getClass().getCanonicalName();
  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  @Autowired
  private TxEventEnvelopeRepository eventRepo;

  @Autowired
  private List<CompensationContext> compensationContexts;

  // use an empty response observer as we don't need the response in client side
  private final StreamObserver<GrpcCompensateCommand> emptyResponseObserver = new StreamObserver<GrpcCompensateCommand>() {
    @Override
    public void onNext(GrpcCompensateCommand value) {
    }

    @Override
    public void onError(Throwable t) {
    }

    @Override
    public void onCompleted() {
    }
  };

  @AfterClass
  public static void tearDown() throws Exception {
    clientChannel.shutdown();
  }

  @Test
  public void persistsEvent() throws Exception {
    StreamObserver<GrpcTxEvent> requestObserver = stub.callbackCommand(emptyResponseObserver);
    requestObserver.onNext(someGrpcEvent(TxStartedEvent));
    // use the asynchronous stub need to wait for some time
    await().atMost(1, SECONDS).until(() -> eventRepo.findByEventGlobalTxId(globalTxId) != null);

    TxEventEnvelope envelope = eventRepo.findByEventGlobalTxId(globalTxId);

    assertThat(envelope.serviceName(), is(serviceName));
    assertThat(envelope.instanceId(), is(instanceId));
    assertThat(envelope.globalTxId(), is(globalTxId));
    assertThat(envelope.localTxId(), is(localTxId));
    assertThat(envelope.parentTxId(), is(parentTxId));
    assertThat(envelope.type(), Is.is(TxStartedEvent.name()));
    assertThat(envelope.compensationMethod(), is(compensationMethod));
    assertThat(envelope.payloads(), is(payload.getBytes()));
  }

  @Test
  public void doNotCompensateDuplicateTxOnFailure() throws Exception {
    // duplicate events with same content but different timestamp
    eventRepo.save(eventEnvelopeOf(TxStartedEvent, localTxId, parentTxId, "service a".getBytes(), "method a"));
    eventRepo.save(eventEnvelopeOf(TxStartedEvent, localTxId, parentTxId, "service a".getBytes(), "method a"));
    eventRepo.save(eventEnvelopeOf(EventType.TxEndedEvent, new byte[0], "method a"));

    String localTxId1 = UUID.randomUUID().toString();
    eventRepo.save(eventEnvelopeOf(TxStartedEvent, localTxId1, UUID.randomUUID().toString(), "service b".getBytes(), "method b"));
    eventRepo.save(eventEnvelopeOf(EventType.TxEndedEvent, new byte[0], "method b"));

    StreamObserver<GrpcTxEvent> requestObserver = stub.callbackCommand(emptyResponseObserver);
    requestObserver.onNext(someGrpcEvent(TxAbortedEvent));

    await().atMost(1, SECONDS).until(() -> compensationContexts.size() > 1);
    assertThat(compensationContexts, containsInAnyOrder(
        new CompensationContext(globalTxId, this.localTxId, "method a", "service a".getBytes()),
        new CompensationContext(globalTxId, localTxId1, "method b", "service b".getBytes())
    ));
  }

  private GrpcTxEvent someGrpcEvent(EventType type) {
    return GrpcTxEvent.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setTimestamp(System.currentTimeMillis())
        .setGlobalTxId(this.globalTxId)
        .setLocalTxId(this.localTxId)
        .setParentTxId(this.parentTxId)
        .setType(type.name())
        .setCompensationMethod(getClass().getCanonicalName())
        .setPayloads(ByteString.copyFrom(payload.getBytes()))
        .build();
  }

  private TxEventEnvelope eventEnvelopeOf(EventType eventType, byte[] payloads, String compensationMethod) {
    return eventEnvelopeOf(eventType, UUID.randomUUID().toString(), UUID.randomUUID().toString(), payloads, compensationMethod);
  }

  private TxEventEnvelope eventEnvelopeOf(EventType eventType, String localTxId, String parentTxId, byte[] payloads, String compensationMethod) {
    return new TxEventEnvelope(new TxEvent(
        serviceName,
        instanceId,
        new Date(),
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
      return event ->
          compensationContexts.add(new CompensationContext(event.globalTxId(), event.localTxId(), event.compensationMethod(), event.payloads()));
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
