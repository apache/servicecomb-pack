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

package org.apache.servicecomb.saga.alpha.server.tcc.jpa;

import org.apache.servicecomb.saga.common.TransactionStatus;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccCoordinatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccParticipatedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionEndedEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcTccTransactionStartedEvent;

public class EventConverter {

  public static ParticipatedEvent convertToParticipatedEvent(GrpcTccParticipatedEvent request) {
    return new ParticipatedEvent(
        request.getServiceName(),
        request.getInstanceId(),
        request.getGlobalTxId(),
        request.getLocalTxId(),
        request.getParentTxId(),
        request.getConfirmMethod(),
        request.getCancelMethod(),
        request.getStatus()
    );
  }

  public static GlobalTxEvent convertToGlobalTxEvent(GrpcTccTransactionStartedEvent request) {
    return new GlobalTxEvent(
        request.getServiceName(),
        request.getInstanceId(),
        request.getGlobalTxId(),
        request.getLocalTxId(),
        request.getParentTxId(),
        TccTxType.STARTED.name(),
        TransactionStatus.Succeed.name()
    );
  }

  public static GlobalTxEvent convertToGlobalTxEvent(GrpcTccTransactionEndedEvent request) {
    return new GlobalTxEvent(
        request.getServiceName(),
        request.getInstanceId(),
        request.getGlobalTxId(),
        request.getLocalTxId(),
        request.getParentTxId(),
        TccTxType.ENDED.name(),
        request.getStatus()
    );
  }

  public static TccTxEvent convertToTccTxEvent(GrpcTccTransactionStartedEvent event) {
    return new TccTxEvent(
        event.getServiceName(),
        event.getInstanceId(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        event.getParentTxId(),
        TccTxType.STARTED.name(),
        TransactionStatus.Succeed.name());
  }

  public static TccTxEvent convertToTccTxEvent(GrpcTccTransactionEndedEvent event) {
    return new TccTxEvent(event.getServiceName(),
        event.getInstanceId(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        event.getParentTxId(),
        TccTxType.STARTED.name(),
        event.getStatus());
  }

  public static TccTxEvent convertToTccTxEvent(GrpcTccParticipatedEvent event) {
    return new TccTxEvent(event.getServiceName(),
        event.getInstanceId(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        event.getParentTxId(),
        TccTxType.PARTICIPATED.name(),
        toMethodInfo(event.getCancelMethod(), event.getConfirmMethod()),
        event.getStatus());
  }

  public static TccTxEvent convertToTccTxEvent(GrpcTccCoordinatedEvent event) {
    return new TccTxEvent( event.getServiceName(),
        event.getInstanceId(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        event.getParentTxId(),
        TccTxType.COORDINATED.name(),
        event.getMethodName(),
        event.getStatus());
  }

  public static TccTxEvent convertToTccTxEvent(GlobalTxEvent event) {
    return new TccTxEvent(event.getServiceName(),
        event.getInstanceId(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        event.getParentTxId(),
        event.getTxType(),
        event.getStatus());
  }

  public static TccTxEvent convertToTccTxEvent(ParticipatedEvent event) {
    return new TccTxEvent(event.getServiceName(),
        event.getInstanceId(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        event.getParentTxId(),
        TccTxType.PARTICIPATED.name(),
        toMethodInfo(event.getConfirmMethod(), event.getCancelMethod()),
        event.getStatus());
  }

  public static ParticipatedEvent convertToParticipatedEvent(TccTxEvent event) {
    return new ParticipatedEvent(
        event.getServiceName(),
        event.getInstanceId(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        event.getParentTxId(),
        getMethodName(event.getMethodInfo(),true),
        getMethodName(event.getMethodInfo(), false),
        event.getStatus(),
        event.getCreationTime(),
        event.getLastModified()
    );
  }

  public static String toMethodInfo(String confirmMethod, String cancelMethod) {
    return "confirm=" + confirmMethod + ",cancel=" + cancelMethod;
  }

  public static String getMethodName(String methodInfo, boolean isConfirm) {
    String[] methods = methodInfo.split(",");
    if (methods.length == 2 && methods[0].startsWith("confirm=") && methods[1].startsWith("cancel=")) {
      String confirmMethod = methods[0].substring(8);
      String cancelMethod = methods[1].substring(7);
      if (isConfirm) return confirmMethod;
      else return cancelMethod;
    } else {
      throw new IllegalArgumentException("MethodInfo: " + methodInfo + ",  has some bad formats.");
    }

  }
}
