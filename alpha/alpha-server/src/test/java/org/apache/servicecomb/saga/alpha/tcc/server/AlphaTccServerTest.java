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

package org.apache.servicecomb.saga.alpha.tcc.server;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.PostConstruct;
import org.apache.servicecomb.saga.alpha.tcc.server.common.AlphaTccApplication;
import org.apache.servicecomb.saga.alpha.tcc.server.common.Bootstrap;
import org.apache.servicecomb.saga.alpha.tcc.server.common.GrpcBootstrap;
import org.apache.servicecomb.saga.alpha.tcc.server.common.GrpcTccServerConfig;
import org.apache.servicecomb.saga.alpha.tcc.server.common.TccCoordinateCommandStreamObserver;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceBlockingStub;
import org.apache.servicecomb.saga.pack.contract.grpc.TccEventServiceGrpc.TccEventServiceStub;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {AlphaTccApplication.class},
    properties = {
        "alpha.server.host=0.0.0.0",
        "alpha.server.port=8098",
        "alpha.event.pollingInterval=1"
    })
public class AlphaTccServerTest {

  @Autowired
  private GrpcTccServerConfig grpcTccServerConfig;

  private static GrpcTccServerConfig serverConfig;
  @PostConstruct
  public void init() {
    serverConfig = grpcTccServerConfig;
    server = new GrpcBootstrap(serverConfig, new GrpcTccEventService());
    new Thread(server::start).start();
  }

  private static final int port = 8090;
  private  static Bootstrap server;
  protected static ManagedChannel clientChannel;

  private final TccEventServiceStub asyncStub = TccEventServiceGrpc.newStub(clientChannel);

  private final TccEventServiceBlockingStub blockingStub = TccEventServiceGrpc.newBlockingStub(clientChannel);

  private static final Queue<GrpcTccCoordinateCommand> receivedCommands = new ConcurrentLinkedQueue<>();

  private final TccCoordinateCommandStreamObserver commandStreamObserver =
      new TccCoordinateCommandStreamObserver(this::onCompensation, receivedCommands);

  private final String globalTxId = UUID.randomUUID().toString();
  private final String localTxId = UUID.randomUUID().toString();
  private final String parentTxId = UUID.randomUUID().toString();
  private final String compensationMethod = getClass().getCanonicalName();

  private final String serviceName = uniquify("serviceName");
  private final String instanceId = uniquify("instanceId");

  private final GrpcServiceConfig serviceConfig = GrpcServiceConfig.newBuilder()
      .setServiceName(serviceName)
      .setInstanceId(instanceId)
      .build();

  @BeforeClass
  public static void setupClientChannel() {
    clientChannel = NettyChannelBuilder.forAddress("localhost", port).usePlaintext().build();
  }

  @AfterClass
  public static void tearDown() {
    clientChannel.shutdown();
    clientChannel = null;
  }

  @Before
  public void before() {
    System.out.println(" globalTxId " + globalTxId);
  }

  @After
  public void after() {
//    blockingStub.onDisconnected(serviceConfig);
  }

  @Test
  public void assertOnConnect() {
//    asyncStub.onConnected(serviceConfig, commandStreamObserver);
  }

  private GrpcAck onCompensation(GrpcTccCoordinateCommand command) {
    return GrpcAck.newBuilder().setAborted(false).build();
  }

}
