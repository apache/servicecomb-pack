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

package org.apache.servicecomb.saga.alpha.server.tcc;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import io.grpc.ManagedChannel;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.servicecomb.saga.alpha.server.tcc.callback.GrpcOmegaTccCallback;
import org.apache.servicecomb.saga.alpha.server.tcc.callback.OmegaCallbacksRegistry;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxType;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.EventConverter;
import org.apache.servicecomb.saga.alpha.server.tcc.service.TccTxEventRepository;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionStartedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceBlockingStub;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceStub;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AlphaTccServerTestBase {

  protected static ManagedChannel clientChannel;

  private final TccEventServiceStub asyncStub = TccEventServiceGrpc.newStub(clientChannel);

  private final TccEventServiceBlockingStub blockingStub = TccEventServiceGrpc.newBlockingStub(clientChannel);

  private final Queue<GrpcTccCoordinateCommand> receivedCommands = new ConcurrentLinkedQueue<>();

  private final TccCoordinateCommandStreamObserver commandStreamObserver =
      new TccCoordinateCommandStreamObserver(this::onReceivedCoordinateCommand, receivedCommands);

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String confirmMethod = "confirm";
  private final String cancelMethod = "cancel";

  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final GrpcServiceConfig serviceConfig = GrpcServiceConfig.newBuilder()
      .setServiceName(serviceName)
      .setInstanceId(instanceId)
      .build();

  @Autowired
  private TccTxEventRepository tccTxEventRepository;


  @AfterClass
  public static void tearDown() {
    clientChannel.shutdown();
    clientChannel = null;
  }

  @After
  public void after() {
    blockingStub.onDisconnected(serviceConfig);
    tccTxEventRepository.deleteAll();
  }

  @Test
  public void assertOnConnect() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    assertThat(
        OmegaCallbacksRegistry.retrieve(serviceName, instanceId), is(instanceOf(GrpcOmegaTccCallback.class))
    );
  }

  @Test
  public void assertOnDisConnect() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    assertThat(
        OmegaCallbacksRegistry.retrieve(serviceName, instanceId), is(instanceOf(GrpcOmegaTccCallback.class))
    );
    blockingStub.onDisconnected(serviceConfig);
    await().atMost(2, SECONDS).until(()->commandStreamObserver.isCompleted());
  }

  private void awaitUntilConnected() {
    await().atMost(2, SECONDS).until(() -> null != (OmegaCallbacksRegistry.getRegistry().get(serviceName)));
  }

  @Test
  public void assertOnTransactionStart() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.onTccTransactionEnded(newTxEnd("Succeed"));
    List<TccTxEvent> events = tccTxEventRepository.findByGlobalTxId(globalTxId).get();
    assertThat(events.size(),  is(2));

    Iterator<TccTxEvent> iterator = events.iterator();
    TccTxEvent event = iterator.next();
    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(localTxId));
    assertThat(event.getInstanceId(), is(instanceId));
    assertThat(event.getServiceName(), is(serviceName));
    assertThat(event.getTxType(), is(TccTxType.STARTED.name()));
    assertThat(event.getStatus(), is(TransactionStatus.Succeed.name()));

    event = iterator.next();
    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(localTxId));
    assertThat(event.getInstanceId(), is(instanceId));
    assertThat(event.getServiceName(), is(serviceName));
    assertThat(event.getTxType(), is(TccTxType.ENDED.name()));
    assertThat(event.getStatus(), is(TransactionStatus.Succeed.name()));
  }

  @Test
  public void assertOnParticipated() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    blockingStub.participate(newParticipatedEvent("Succeed"));
    blockingStub.participate(newParticipatedEvent("Succeed"));
    List<TccTxEvent> events = tccTxEventRepository.findByGlobalTxId(globalTxId).get();
    assertThat(events.size(),  is(1));
    TccTxEvent event = events.iterator().next();
    assertThat(event.getGlobalTxId(), is(globalTxId));
    assertThat(event.getLocalTxId(), is(localTxId));
    assertThat(event.getInstanceId(), is(instanceId));
    assertThat(event.getServiceName(), is(serviceName));
    assertThat(EventConverter.getMethodName(event.getMethodInfo(), true), is(confirmMethod));
    assertThat(EventConverter.getMethodName(event.getMethodInfo(), false), is(cancelMethod));
    assertThat(event.getStatus(), is("Succeed"));
  }

  @Test
  public void assertOnTccTransactionSucceedEnded() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.participate(newParticipatedEvent("Succeed"));
    blockingStub.onTccTransactionEnded(newTxEnd("Succeed"));

    await().atMost(2, SECONDS).until(() -> !receivedCommands.isEmpty());
    assertThat(receivedCommands.size(), is(1));
    GrpcTccCoordinateCommand command = receivedCommands.poll();
    assertThat(command.getMethod(), is("confirm"));
    assertThat(command.getGlobalTxId(), is(globalTxId));
    assertThat(command.getServiceName(), is(serviceName));

    GrpcAck result = blockingStub.onTccCoordinated(newCoordinatedEvent("Succeed", "Confirm"));
    assertThat(result.getAborted(), is(false));
  }

  @Test
  public void assertOnTccTransactionFailedEnded() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.participate(newParticipatedEvent("Succeed"));
    blockingStub.onTccTransactionEnded(newTxEnd("Failed"));

    await().atMost(2, SECONDS).until(() -> !receivedCommands.isEmpty());
    assertThat(receivedCommands.size(), is(1));
    GrpcTccCoordinateCommand command = receivedCommands.poll();
    assertThat(command.getMethod(), is("cancel"));
    assertThat(command.getGlobalTxId(), is(globalTxId));
    assertThat(command.getServiceName(), is(serviceName));
    assertThat(commandStreamObserver.isCompleted(), is(false));
  }

  @Test
  public void assertOnCallbackNotExist() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();

    OmegaCallbacksRegistry.getRegistry().remove(serviceName);
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.participate(newParticipatedEvent("Succeed"));
    GrpcAck result = blockingStub.onTccTransactionEnded(newTxEnd("Succeed"));
    assertThat(result.getAborted(), is(true));
  }

  @Test
  public void assertOnCallbacksExecuteError() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    awaitUntilConnected();

    OmegaCallbacksRegistry.getRegistry().get(serviceName).put(instanceId, new GrpcOmegaTccCallback(null));
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.participate(newParticipatedEvent("Succeed"));
    GrpcAck result = blockingStub.onTccTransactionEnded(newTxEnd("Succeed"));

    assertThat(result.getAborted(), is(true));
    assertThat(OmegaCallbacksRegistry.getRegistry().get(serviceName).size(), is(0));
  }

  @Test
  public void assertOnSwitchOtherCallbackInstance() {
    asyncStub.onConnected(serviceConfig, commandStreamObserver);
    GrpcServiceConfig config = GrpcServiceConfig.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(uniquify("instanceId"))
        .build();
    asyncStub.onConnected(config, commandStreamObserver);

    await().atMost(1, SECONDS).until(() -> (OmegaCallbacksRegistry.getRegistry().get(serviceName) != null));
    await().atMost(1, SECONDS).until(() -> (OmegaCallbacksRegistry.getRegistry().get(serviceName).size() == 2));

    OmegaCallbacksRegistry.getRegistry().get(serviceName).remove(instanceId);
    blockingStub.onTccTransactionStarted(newTxStart());
    blockingStub.participate(newParticipatedEvent("Succeed"));
    GrpcAck result = blockingStub.onTccTransactionEnded(newTxEnd("Succeed"));

    await().atMost(2, SECONDS).until(() -> !receivedCommands.isEmpty());
    assertThat(receivedCommands.size(), is(1));
    GrpcTccCoordinateCommand command = receivedCommands.poll();
    assertThat(command.getMethod(), is("confirm"));
    assertThat(command.getGlobalTxId(), is(globalTxId));
    assertThat(command.getServiceName(), is(serviceName));

    assertThat(result.getAborted(), is(false));
  }

  private GrpcTccParticipatedEvent newParticipatedEvent(String status) {
    return GrpcTccParticipatedEvent.newBuilder()
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setCancelMethod(cancelMethod)
        .setConfirmMethod(confirmMethod)
        .setStatus(status)
        .build();
  }

  private GrpcTccTransactionStartedEvent newTxStart() {
    return GrpcTccTransactionStartedEvent.newBuilder()
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setLocalTxId(localTxId)
        .build();
  }

  private GrpcTccTransactionEndedEvent newTxEnd(String status) {
    return GrpcTccTransactionEndedEvent.newBuilder()
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setLocalTxId(localTxId)
        .setStatus(status)
        .build();
  }

  private GrpcTccCoordinatedEvent newCoordinatedEvent(String status, String method) {
    return GrpcTccCoordinatedEvent.newBuilder()
        .setGlobalTxId(globalTxId)
        .setLocalTxId(localTxId)
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .setMethodName(method)
        .setStatus(status)
        .build();
  }

  private GrpcAck onReceivedCoordinateCommand(GrpcTccCoordinateCommand command) {
    return GrpcAck.newBuilder().setAborted(false).build();
  }
}
