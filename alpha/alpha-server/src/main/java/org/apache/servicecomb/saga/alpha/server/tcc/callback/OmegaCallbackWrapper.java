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

import org.apache.servicecomb.saga.alpha.server.tcc.event.ParticipatedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.registry.OmegaCallbacksRegistry;
import org.apache.servicecomb.saga.common.TransactionStatus;

public class OmegaCallbackWrapper implements OmegaCallback {

  @Override
  public void invoke(ParticipatedEvent event, TransactionStatus status) {
    OmegaCallback omegaCallback = OmegaCallbacksRegistry.retrieve(event.getServiceName(), event.getInstanceId());
    try {
      omegaCallback.invoke(event, status);
    } catch (Exception ex) {
      OmegaCallbacksRegistry.remove(event.getServiceName(), event.getInstanceId());
      throw ex;
    }
  }
}
