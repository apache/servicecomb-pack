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

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import java.lang.invoke.MethodHandles;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.apache.servicecomb.pack.alpha.core.exception.CompensateAckFailedException;
import org.apache.servicecomb.pack.alpha.core.exception.CompensateConnectException;
import org.apache.servicecomb.pack.alpha.core.fsm.CompensateAckType;
import org.apache.servicecomb.pack.contract.grpc.GrpcCompensateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GrpcOmegaCallback implements OmegaCallback {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final StreamObserver<GrpcCompensateCommand> observer;
  private CompensateAckCountDownLatch compensateAckCountDownLatch;

  GrpcOmegaCallback(StreamObserver<GrpcCompensateCommand> observer) {
    this.observer = observer;
  }

  @Override
  public void compensate(TxEvent event) {
    compensateAckCountDownLatch = new CompensateAckCountDownLatch(1);
    try {
      GrpcCompensateCommand command = GrpcCompensateCommand.newBuilder()
          .setGlobalTxId(event.globalTxId())
          .setLocalTxId(event.localTxId())
          .setParentTxId(event.parentTxId() == null ? "" : event.parentTxId())
          .setCompensationMethod(event.compensationMethod())
          .setPayloads(ByteString.copyFrom(event.payloads()))
          .build();
      observer.onNext(command);
      compensateAckCountDownLatch.await();
      if (compensateAckCountDownLatch.getType() == CompensateAckType.Disconnected) {
        throw new CompensateConnectException("Omega connect exception");
      }else{
        LOG.debug("compensate ack "+ compensateAckCountDownLatch.getType().name());
        if(compensateAckCountDownLatch.getType() == CompensateAckType.Failed){
          throw new CompensateAckFailedException("An exception is thrown inside the compensation method");
        }
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      compensateAckCountDownLatch = null;
    }
  }

  @Override
  public void disconnect() {
    observer.onCompleted();
    if (compensateAckCountDownLatch != null) {
      compensateAckCountDownLatch.countDown(CompensateAckType.Disconnected);
    }
  }

  @Override
  public void getAck(CompensateAckType type) {
    if (compensateAckCountDownLatch != null) {
      compensateAckCountDownLatch.countDown(type);
    }
  }

  @Override
  public boolean isWaiting(){
    return compensateAckCountDownLatch == null ? false : true;
  }
}
