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

import io.grpc.stub.StreamObserver;
import java.util.Queue;
import java.util.function.Consumer;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinateCommand;

public class TccCoordinateCommandStreamObserver implements StreamObserver<GrpcTccCoordinateCommand> {

  private Queue<GrpcTccCoordinateCommand> receivedCommands;
  private Consumer<GrpcTccCoordinateCommand> consumer;

  public boolean isCompleted() {
    return completed;
  }

  private boolean completed = false;

  public TccCoordinateCommandStreamObserver(Consumer<GrpcTccCoordinateCommand> consumer,
      Queue<GrpcTccCoordinateCommand> receivedCommands) {
    this.consumer = consumer;
    this.receivedCommands = receivedCommands;
  }

  @Override
  public void onNext(GrpcTccCoordinateCommand value) {
    consumer.accept(value);
    receivedCommands.add(value);
  }

  @Override
  public void onError(Throwable t) {
  }

  @Override
  public void onCompleted() {
    completed = true;
  }
}
