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

package org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka;

import static org.apache.servicecomb.pack.common.EventType.TxCompensateEvent;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.servicecomb.pack.alpha.core.OmegaCallback;
import org.apache.servicecomb.pack.alpha.core.TxEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.TxEntity;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka.SpringAkkaExtension.SpringExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class SpringAkkaExtension extends AbstractExtensionId<SpringExt> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final SpringAkkaExtension SPRING_EXTENSION_PROVIDER = new SpringAkkaExtension();

  @Override
  public SpringExt createExtension(ExtendedActorSystem system) {
    return new SpringExt();
  }

  public static class SpringExt implements Extension {

    private static final String omegaCallbackBeanName = "omegaCallback";
    private volatile ApplicationContext applicationContext;
    private OmegaCallback omegaCallback;

    public void compensate(TxEntity txEntity)
        throws InterruptedException, ExecutionException, TimeoutException {
      if (txEntity.getReverseTimeout() > 0) {
        CompletableFuture.runAsync(() -> doCompensate(txEntity))
            .get(txEntity.getReverseTimeout(), TimeUnit.SECONDS);
      } else {
        doCompensate(txEntity);
      }
    }

    private void doCompensate(TxEntity txEntity) {
      if (applicationContext != null) {
        if (applicationContext.containsBean(omegaCallbackBeanName)) {
          omegaCallback = applicationContext.getBean(omegaCallbackBeanName, OmegaCallback.class);
          TxEvent event = new TxEvent(
              txEntity.getServiceName(),
              txEntity.getInstanceId(),
              txEntity.getGlobalTxId(),
              txEntity.getLocalTxId(),
              txEntity.getParentTxId(),
              TxCompensateEvent.name(),
              txEntity.getCompensationMethod(),
              txEntity.getPayloads());
          omegaCallback.compensate(event);
        } else {
          LOG.warn("Spring Bean {} doesn't exist in ApplicationContext", omegaCallbackBeanName);
        }
      } else {
        LOG.warn("Spring ApplicationContext is null");
      }
    }

    public void initialize(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }
  }
}
