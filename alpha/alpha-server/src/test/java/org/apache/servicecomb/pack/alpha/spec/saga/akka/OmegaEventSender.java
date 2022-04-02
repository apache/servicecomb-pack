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

package org.apache.servicecomb.pack.alpha.spec.saga.akka;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.contract.grpc.GrpcAck;
import org.apache.servicecomb.pack.contract.grpc.GrpcCompensateCommand;
import org.apache.servicecomb.pack.contract.grpc.GrpcServiceConfig;
import org.apache.servicecomb.pack.contract.grpc.ServerMeta;
import org.apache.servicecomb.pack.contract.grpc.TxEventServiceGrpc;
import org.apache.servicecomb.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceBlockingStub;
import org.apache.servicecomb.pack.contract.grpc.TxEventServiceGrpc.TxEventServiceStub;

public class OmegaEventSender {
  GrpcServiceConfig serviceConfig;
  protected ManagedChannel clientChannel;
  private TxEventServiceStub asyncStub;
  private TxEventServiceBlockingStub blockingStub;
  private final Queue<GrpcCompensateCommand> receivedCommands = new ConcurrentLinkedQueue<>();
  private final CompensationStreamObserver compensateResponseObserver = new CompensationStreamObserver(
      this::onCompensation);
  private Map<String, Map<String, OmegaCallback>> omegaCallbacks;
  private OmegaEventSagaSimulator omegaEventSagaSimulator;

  private String serviceName;
  private String instanceId;

  public void configClient(ManagedChannel clientChannel){
    this.clientChannel = clientChannel;
    this.asyncStub = TxEventServiceGrpc.newStub(clientChannel);
    this.blockingStub = TxEventServiceGrpc.newBlockingStub(clientChannel);
  }

  public void shutdown(){
    this.clientChannel.shutdown();
    this.clientChannel = null;
  }

  public void onConnected(){
    serviceConfig = GrpcServiceConfig.newBuilder()
        .setServiceName(serviceName)
        .setInstanceId(instanceId)
        .build();
    asyncStub.onConnected(compensateResponseObserver).onNext(serviceConfig);
    omegaEventSagaSimulator = OmegaEventSagaSimulator.builder().serviceName(serviceName).instanceId(instanceId).build();

  }

  public void onDisconnected(){
    blockingStub.onDisconnected(serviceConfig);
  }

  public ServerMeta onGetServerMeta(){
    return blockingStub.onGetServerMeta(serviceConfig);
  }

  public void setOmegaCallbacks(
      Map<String, Map<String, OmegaCallback>> omegaCallbacks) {
    this.omegaCallbacks = omegaCallbacks;
  }

  public Queue<GrpcCompensateCommand> getReceivedCommands() {
    return receivedCommands;
  }

  public TxEventServiceBlockingStub getBlockingStub() {
    return blockingStub;
  }

  public Map<String, Map<String, OmegaCallback>> getOmegaCallbacks(){
    return omegaCallbacks;
  }

  public GrpcServiceConfig getServiceConfig() {
    return serviceConfig;
  }

  public void reset(){
    receivedCommands.clear();
  }

  public OmegaEventSagaSimulator getOmegaEventSagaSimulator(){
    return omegaEventSagaSimulator;
  }

  private class CompensationStreamObserver implements StreamObserver<GrpcCompensateCommand> {
    private final Consumer<GrpcCompensateCommand> consumer;
    private boolean completed = false;

    private CompensationStreamObserver() {
      this(command -> {});
    }

    private CompensationStreamObserver(Consumer<GrpcCompensateCommand> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void onNext(GrpcCompensateCommand command) {
      // intercept received command
      consumer.accept(command);
      receivedCommands.add(command);
    }

    @Override
    public void onError(Throwable t) {
    }

    @Override
    public void onCompleted() {
      completed = true;
    }

    boolean isCompleted() {
      return completed;
    }
  }

  private GrpcAck onCompensation(GrpcCompensateCommand command) {
    return blockingStub.onTxEvent(omegaEventSagaSimulator.txCompensatedEvent(command.getGlobalTxId(),command.getLocalTxId(),command.getParentTxId()));
  }

  public static OmegaEventSender.Builder builder() {
    return new OmegaEventSender.Builder();
  }

  public static final class Builder {

    private String serviceName = uniquify("omega-serviceName");
    private String instanceId = uniquify("omega-instanceId");

    public OmegaEventSender.Builder serviceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public OmegaEventSender.Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public OmegaEventSender build() {
      OmegaEventSender omegaEventSender = new OmegaEventSender();
      omegaEventSender.instanceId = this.instanceId;
      omegaEventSender.serviceName = this.serviceName;
      return omegaEventSender;
    }
  }
}
