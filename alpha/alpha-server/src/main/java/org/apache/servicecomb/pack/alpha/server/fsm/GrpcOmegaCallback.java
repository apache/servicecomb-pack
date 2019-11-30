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

package org.apache.servicecomb.pack.alpha.server.fsm;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.apache.servicecomb.pack.alpha.core.exception.CompensateAskFailedException;
import org.apache.servicecomb.pack.alpha.core.exception.CompensateConnectException;
import org.apache.servicecomb.pack.alpha.core.fsm.CompensateAskType;
import org.apache.servicecomb.pack.contract.grpc.GrpcCompensateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GrpcOmegaCallback implements OmegaCallback {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final StreamObserver<GrpcCompensateCommand> observer;
  private CompensateAskWait compensateAskWait;

  GrpcOmegaCallback(StreamObserver<GrpcCompensateCommand> observer) {
    this.observer = observer;
  }

  @Override
  public void compensate(TxEvent event) {
    compensateAskWait = new CompensateAskWait(1);
    try {
      GrpcCompensateCommand command = GrpcCompensateCommand.newBuilder()
          .setGlobalTxId(event.globalTxId())
          .setLocalTxId(event.localTxId())
          .setParentTxId(event.parentTxId() == null ? "" : event.parentTxId())
          .setCompensationMethod(event.compensationMethod())
          .setPayloads(ByteString.copyFrom(event.payloads()))
          .build();
      observer.onNext(command);
      compensateAskWait.await();
      if (compensateAskWait.getType() == CompensateAskType.Disconnected) {
        throw new CompensateConnectException("Omega connect exception");
      }else{
        LOG.info("compensate ask "+compensateAskWait.getType().name());
        if(compensateAskWait.getType() == CompensateAskType.Failed){
          throw new CompensateAskFailedException("An exception is thrown inside the compensation method");
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      compensateAskWait = null;
    }
  }

  @Override
  public void disconnect() {
    observer.onCompleted();
    if (compensateAskWait != null) {
      compensateAskWait.countDown(CompensateAskType.Disconnected);
    }
  }

  @Override
  public void ask(CompensateAskType type) {
    if (compensateAskWait != null) {
      compensateAskWait.countDown(type);
    }
  }

  @Override
  public boolean isWaiting(){
    return compensateAskWait == null ? false : true;
  }
}
