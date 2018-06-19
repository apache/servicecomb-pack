/*
 *   Copyright 2017 Huawei Technologies Co., Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.servicecomb.saga.omega.transport.servicecomb;

import java.util.List;

import org.apache.servicecomb.core.BootListener;
import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.handler.ConsumerHandlerManager;
import org.apache.servicecomb.core.handler.ProducerHandlerManager;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SagaBootListener implements BootListener {
  @Autowired(required = false)
  private OmegaContext omegaContext;

  @Override
  public void onBootEvent(BootEvent bootEvent) {
    if (EventType.AFTER_HANDLER.equals(bootEvent.getEventType())) {
      for (List<Handler> handlers : ConsumerHandlerManager.INSTANCE.values()) {
        injectOmegaContextIntoConsumerHandler(handlers);
      }

      for (List<Handler> handlers : ProducerHandlerManager.INSTANCE.values()) {
        injectOmegaContextIntoProducerHandler(handlers);
      }
    }
  }

  private void injectOmegaContextIntoConsumerHandler(List<Handler> handlers) {
    for (Handler handler : handlers) {
      if (handler.getClass().equals(SagaConsumerHandler.class)) {
        ((SagaConsumerHandler) handler).setOmegaContext(omegaContext);
        return;
      }
    }
  }

  private void injectOmegaContextIntoProducerHandler(List<Handler> handlers) {
    for (Handler handler : handlers) {
      if (handler.getClass().equals(SagaProviderHandler.class)) {
        ((SagaProviderHandler) handler).setOmegaContext(omegaContext);
        return;
      }
    }
  }
}