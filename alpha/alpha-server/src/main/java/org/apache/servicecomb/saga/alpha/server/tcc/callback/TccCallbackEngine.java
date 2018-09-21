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

package org.apache.servicecomb.saga.alpha.server.tcc.callback;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.servicecomb.saga.alpha.server.tcc.jpa.GlobalTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxType;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.EventConverter;
import org.apache.servicecomb.saga.alpha.server.tcc.service.TccTxEventRepository;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
@Component
public class TccCallbackEngine implements CallbackEngine {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private OmegaCallbackWrapper omegaCallbackWrapper;

  @Autowired
  private TccTxEventRepository tccTxEventRepository;

  @Override
  public boolean execute(GlobalTxEvent request) {
    boolean result = true;
    List<TccTxEvent> events = tccTxEventRepository.findByGlobalTxIdAndTxType(request.getGlobalTxId(), TccTxType.PARTICIPATED).orElse(
        Lists.newArrayList());
    for (TccTxEvent event : events) {
      ParticipatedEvent participatedEvent = EventConverter.convertToParticipatedEvent(event);
      try {
        // only invoke the event is succeed
        if (event.getStatus().equals(TransactionStatus.Succeed.toString())) {
          omegaCallbackWrapper.invoke(participatedEvent, TransactionStatus.valueOf(request.getStatus()));
        }
      } catch (Exception ex) {
        logError(participatedEvent, ex);
        result = false;
      }
    }
    return result;
  }

  private void logError(ParticipatedEvent event, Exception ex) {
    LOG.error(
        "Failed to invoke service [{}] instance [{}] with method [{}], global tx id [{}] and local tx id [{}]",
        event.getServiceName(),
        event.getInstanceId(),
        TransactionStatus.Succeed.equals(event.getStatus()) ? event.getConfirmMethod() : event.getCancelMethod(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        ex);
  }
}
