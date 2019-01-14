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

package org.apache.servicecomb.pack.omega.connector.grpc.tcc;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.servicecomb.pack.common.TransactionStatus;
import org.apache.servicecomb.pack.contract.grpc.*;
import org.apache.servicecomb.pack.omega.context.ServiceConfig;
import org.apache.servicecomb.pack.omega.transaction.AlphaResponse;
import org.apache.servicecomb.pack.omega.transaction.tcc.TccMessageHandler;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.*;
import org.apache.servicecomb.pack.omega.transaction.tcc.events.ParticipationStartedEvent;
import org.apache.servicecomb.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceImplBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.grpc.util.MutableHandlerRegistry;

@RunWith(JUnit4.class)
public class GrpcTccClientMessageSenderTest {
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();
  private final GrpcAck ack = GrpcAck.newBuilder().setAborted(false).build();

  private final String globalTxId = uniquify("globalTxId");
  private final String localTxId = uniquify("localTxId");
  private final String parentTxId = uniquify("parentTxId");
  private final String methodName = uniquify("methodName");
  private final String confirmMethod = uniquify("confirmMethod");
  private final String cancelMethod = uniquify("cancleMethod");
  private final String serviceName = uniquify("serviceName");

  private final ServiceConfig serviceConfig = new ServiceConfig(uniquify("Service"));
  private final String address = uniquify("Address");
  private final TccMessageHandler handler = mock(TccMessageHandler.class);
  private GrpcTccClientMessageSender service;

  @Before
  public void setUp() throws Exception {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).
        fallbackHandlerRegistry(serviceRegistry).directExecutor().build().start());

    // Create a client channel and register for automatic graceful shutdown.
    ManagedChannel channel = grpcCleanup.register(
        InProcessChannelBuilder.forName(serverName).directExecutor().build());

    // Create a TccEventServiceStub using the in-process channel;
    service = new GrpcTccClientMessageSender(serviceConfig, channel, address, handler, null);
  }

  @Test
  public void serviceOnDisconnectedTest() {

    final GrpcServiceConfig[] requestCaptor = new GrpcServiceConfig[1];

    TccEventServiceImplBase serviceImpl = new TccEventServiceImplBase() {

      public void onDisconnected(GrpcServiceConfig request, StreamObserver<GrpcAck> responseObserver) {
        requestCaptor[0] = request;
        responseObserver.onNext(ack);
        responseObserver.onCompleted();
      }
    };

    serviceRegistry.addService(serviceImpl);
    service.onDisconnected();

    assertThat(requestCaptor[0].getServiceName(), is(serviceConfig.serviceName()));
    assertThat(requestCaptor[0].getInstanceId(), is(serviceConfig.instanceId()));
  }


  @Test
  public void serviceOnConnectedTest() {
    final GrpcTccCoordinateCommand coordinateCommand =
        GrpcTccCoordinateCommand.newBuilder()
            .setGlobalTxId(globalTxId)
            .setLocalTxId(localTxId)
            .setParentTxId(parentTxId)
            .setMethod(methodName)
            .setServiceName(serviceName)
        .build();

    final GrpcServiceConfig[] requestCaptor = new GrpcServiceConfig[1];

    TccEventServiceImplBase serviceImpl = new TccEventServiceImplBase() {
      @Override
      public void onConnected(GrpcServiceConfig request, StreamObserver<GrpcTccCoordinateCommand> responseObserver) {
        requestCaptor[0] = request;
        // Just send the coordinateCommand back
        responseObserver.onNext(coordinateCommand);
        responseObserver.onCompleted();
      }
    };

    serviceRegistry.addService(serviceImpl);
    service.onConnected();

    assertThat(requestCaptor[0].getServiceName(), is(serviceConfig.serviceName()));
    assertThat(requestCaptor[0].getInstanceId(), is(serviceConfig.instanceId()));

    verify(handler).onReceive(globalTxId, localTxId,parentTxId, methodName);

  }

  @Test
  public void serviceOnTransactionStartTest() {

    final GrpcTccTransactionStartedEvent[] requestCaptor = new GrpcTccTransactionStartedEvent[1];
    TccStartedEvent event = new TccStartedEvent(globalTxId,localTxId);

    TccEventServiceImplBase serviceImpl = new TccEventServiceImplBase() {

      public void onTccTransactionStarted(GrpcTccTransactionStartedEvent request,
          io.grpc.stub.StreamObserver<GrpcAck> responseObserver) {
        requestCaptor[0] = request;
        responseObserver.onNext(ack);
        responseObserver.onCompleted();
      }
    };

    serviceRegistry.addService(serviceImpl);
    AlphaResponse response =service.tccTransactionStart(event);

    assertThat(requestCaptor[0].getServiceName(), is(serviceConfig.serviceName()));
    assertThat(requestCaptor[0].getInstanceId(), is(serviceConfig.instanceId()));
    assertThat(requestCaptor[0].getGlobalTxId(), is(globalTxId));
    assertThat(requestCaptor[0].getLocalTxId(), is(localTxId));
    assertThat(response.aborted(), is(false));
  }

  @Test
  public void serviceOnTransactionEndTest() {

    final GrpcTccTransactionEndedEvent[] requestCaptor = new GrpcTccTransactionEndedEvent[1];
    TccEndedEvent event = new TccEndedEvent(globalTxId,localTxId, TransactionStatus.Failed);

    TccEventServiceImplBase serviceImpl = new TccEventServiceImplBase() {

      public void onTccTransactionEnded(GrpcTccTransactionEndedEvent request,
          io.grpc.stub.StreamObserver<GrpcAck> responseObserver) {
        requestCaptor[0] = request;
        responseObserver.onNext(ack);
        responseObserver.onCompleted();
      }
    };

    serviceRegistry.addService(serviceImpl);
    AlphaResponse response =service.tccTransactionStop(event);

    assertThat(requestCaptor[0].getServiceName(), is(serviceConfig.serviceName()));
    assertThat(requestCaptor[0].getInstanceId(), is(serviceConfig.instanceId()));
    assertThat(requestCaptor[0].getGlobalTxId(), is(globalTxId));
    assertThat(requestCaptor[0].getLocalTxId(), is(localTxId));
    assertThat(requestCaptor[0].getStatus(), is(TransactionStatus.Failed.toString()));
    assertThat(response.aborted(), is(false));
  }

  @Test
  public void serviceOnStartParticipateTest() {

    final GrpcParticipationStartedEvent[] requestCaptor = new GrpcParticipationStartedEvent[1];
    ParticipationStartedEvent event = new ParticipationStartedEvent(globalTxId,localTxId, parentTxId, confirmMethod,
        cancelMethod);

    TccEventServiceImplBase serviceImpl = new TccEventServiceImplBase() {

      public void onParticipationStarted(GrpcParticipationStartedEvent request,
          StreamObserver<GrpcAck> responseObserver) {
        requestCaptor[0] = request;
        responseObserver.onNext(ack);
        responseObserver.onCompleted();
      }
    };

    serviceRegistry.addService(serviceImpl);
    AlphaResponse response =service.participationStart(event);

    assertThat(requestCaptor[0].getServiceName(), is(serviceConfig.serviceName()));
    assertThat(requestCaptor[0].getInstanceId(), is(serviceConfig.instanceId()));
    assertThat(requestCaptor[0].getGlobalTxId(), is(globalTxId));
    assertThat(requestCaptor[0].getLocalTxId(), is(localTxId));
    assertThat(requestCaptor[0].getParentTxId(), is(parentTxId));
    assertThat(response.aborted(), is(false));
  }

  @Test
  public void serviceOnEndParticipateTest() {

    final GrpcParticipationEndedEvent[] requestCaptor = new GrpcParticipationEndedEvent[1];
    ParticipationEndedEvent event = new ParticipationEndedEvent(globalTxId,localTxId, parentTxId, confirmMethod,
        cancelMethod, TransactionStatus.Succeed);

    TccEventServiceImplBase serviceImpl = new TccEventServiceImplBase() {

      public void onParticipationEnded(GrpcParticipationEndedEvent request,
          StreamObserver<GrpcAck> responseObserver) {
        requestCaptor[0] = request;
        responseObserver.onNext(ack);
        responseObserver.onCompleted();
      }
    };

    serviceRegistry.addService(serviceImpl);
    AlphaResponse response =service.participationEnd(event);

    assertThat(requestCaptor[0].getServiceName(), is(serviceConfig.serviceName()));
    assertThat(requestCaptor[0].getInstanceId(), is(serviceConfig.instanceId()));
    assertThat(requestCaptor[0].getGlobalTxId(), is(globalTxId));
    assertThat(requestCaptor[0].getLocalTxId(), is(localTxId));
    assertThat(requestCaptor[0].getParentTxId(), is(parentTxId));
    assertThat(requestCaptor[0].getStatus(), is(TransactionStatus.Succeed.toString()));
    assertThat(response.aborted(), is(false));
  }

  @Test
  public void serviceOnCoordinateTest() {

    final GrpcTccCoordinatedEvent[] requestCaptor = new GrpcTccCoordinatedEvent[1];
    CoordinatedEvent event = new CoordinatedEvent(globalTxId,localTxId, parentTxId, methodName, TransactionStatus.Succeed);

    TccEventServiceImplBase serviceImpl = new TccEventServiceImplBase() {

      public void onTccCoordinated(GrpcTccCoordinatedEvent request,
          io.grpc.stub.StreamObserver<GrpcAck> responseObserver) {
        requestCaptor[0] = request;
        responseObserver.onNext(ack);
        responseObserver.onCompleted();
      }
    };

    serviceRegistry.addService(serviceImpl);
    AlphaResponse response =service.coordinate(event);

    assertThat(requestCaptor[0].getServiceName(), is(serviceConfig.serviceName()));
    assertThat(requestCaptor[0].getInstanceId(), is(serviceConfig.instanceId()));
    assertThat(requestCaptor[0].getGlobalTxId(), is(globalTxId));
    assertThat(requestCaptor[0].getLocalTxId(), is(localTxId));
    assertThat(requestCaptor[0].getMethodName(), is(methodName));
    assertThat(requestCaptor[0].getStatus(), is(TransactionStatus.Succeed.toString()));
    assertThat(response.aborted(), is(false));
  }



}
