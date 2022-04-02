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

package org.apache.servicecomb.pack.alpha.spec.tcc.db.callback;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.GlobalTxEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.jpa.ParticipatedEvent;
import org.apache.servicecomb.pack.alpha.spec.tcc.db.service.TccTxEventRepository;
import org.apache.servicecomb.pack.common.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TccCallbackEngine implements CallbackEngine {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private OmegaCallbackWrapper omegaCallbackWrapper;

  @Autowired
  private TccTxEventRepository tccTxEventRepository;

  @Override
  public boolean execute(GlobalTxEvent request) {
    AtomicBoolean result = new AtomicBoolean(true);
    tccTxEventRepository.findParticipatedByGlobalTxId(request.getGlobalTxId()).ifPresent(e ->
        // Just call the confirm or cancel method of the omega instance
        e.stream().filter(d -> d.getStatus().equals(TransactionStatus.Succeed.name())).forEach(p -> {
          try {
            omegaCallbackWrapper.invoke(p, TransactionStatus.valueOf(request.getStatus()));
          } catch (Exception ex) {
            logError(p, ex);
            result.set(false);
          }
        })
    );
    return result.get();
  }

  private void logError(ParticipatedEvent event, Exception ex) {
    LOG.error(
        "Failed to invoke service [{}] instance [{}] with method [{}], global tx id [{}] and local tx id [{}]",
        event.getServiceName(),
        event.getInstanceId(),
        TransactionStatus.Succeed.name().equals(event.getStatus()) ? event.getConfirmMethod() : event.getCancelMethod(),
        event.getGlobalTxId(),
        event.getLocalTxId(),
        ex);
  }
}
