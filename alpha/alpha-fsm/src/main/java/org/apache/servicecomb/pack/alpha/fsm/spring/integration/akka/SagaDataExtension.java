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

package org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.servicecomb.pack.alpha.fsm.SagaActorState;
import org.apache.servicecomb.pack.alpha.fsm.TransactionType;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.repository.model.GloablTransaction;
import org.apache.servicecomb.pack.alpha.fsm.repository.model.SagaSubTransaction;
import org.apache.servicecomb.pack.alpha.fsm.repository.TransactionRepositoryChannel;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension.SagaDataExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SagaDataExtension extends AbstractExtensionId<SagaDataExt> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final SagaDataExtension SAGA_DATA_EXTENSION_PROVIDER = new SagaDataExtension();
  //TODO We could use test profile the enable this kind feature
  public static boolean autoCleanSagaDataMap = true; // Only for Test

  @Override
  public SagaDataExt createExtension(ExtendedActorSystem system) {
    return new SagaDataExt();
  }

  public static class SagaDataExt implements Extension {

    private final ConcurrentHashMap<String, SagaData> sagaDataMap = new ConcurrentHashMap();
    private String lastGlobalTxId;
    private CleanMemForTest cleanMemForTest = new CleanMemForTest(sagaDataMap);
    private MetricsService metricsService;
    private TransactionRepositoryChannel repositoryChannel;

    public SagaDataExt() {
      // Just to avoid the overflow of the OldGen for stress testing
      // Delete after SagaData persistence
      if (autoCleanSagaDataMap) {
        new Thread(cleanMemForTest).start();
      }
    }

    public void putSagaData(String globalTxId, SagaData sagaData) {
      lastGlobalTxId = globalTxId;
      sagaDataMap.put(globalTxId, sagaData);
    }

    public void stopSagaData(String globalTxId, SagaData sagaData) {
      // TODO save SagaDate to database and clean sagaDataMap
      this.putSagaData(globalTxId, sagaData);
      lastGlobalTxId = globalTxId;
      if (sagaData.getLastState() == SagaActorState.COMMITTED) {
        this.metricsService.metrics().doCommitted();
      } else if (sagaData.getLastState() == SagaActorState.COMPENSATED) {
        this.metricsService.metrics().doCompensated();
      } else if (sagaData.getLastState() == SagaActorState.SUSPENDED) {
        this.metricsService.metrics().doSuspended();
      }
      List<SagaSubTransaction> subTransactions = new ArrayList();
      sagaData.getTxEntityMap().forEach((k,v)->{
        subTransactions.add(SagaSubTransaction.builder()
            .parentTxId(v.getParentTxId())
            .localTxId(v.getLocalTxId())
            .beginTime(v.getBeginTime())
            .endTime(v.getEndTime())
            .state(v.getState())
            .build());
      });
      GloablTransaction record = GloablTransaction.builder()
          .type(TransactionType.SAGA)
          .serviceName(sagaData.getServiceName())
          .instanceId(sagaData.getInstanceId())
          .globalTxId(sagaData.getGlobalTxId())
          .beginTime(sagaData.getBeginTime())
          .endTime(sagaData.getEndTime())
          .state(sagaData.getLastState())
          .subTxSize(sagaData.getTxEntityMap().size())
          .subTransactions(subTransactions)
          .events(sagaData.getEvents())
          .build();
      repositoryChannel.send(record);
    }

    public SagaData getSagaData(String globalTxId) {
      // TODO If globalTxId does not exist in sagaDataMap then
      //  load from the database
      return sagaDataMap.get(globalTxId);
    }

    // Only test
    public void clearSagaData() {
      lastGlobalTxId = null;
      sagaDataMap.clear();
    }

    public SagaData getLastSagaData() {
      return getSagaData(lastGlobalTxId);
    }

    public void doSagaBeginCounter() {
      this.metricsService.metrics().doSagaBeginCounter();
    }

    public void doSagaEndCounter() {
      this.metricsService.metrics().doSagaEndCounter();
    }

    public void doSagaAvgTime(long time) {
      this.metricsService.metrics().doSagaAvgTime(time);
    }

    public void setMetricsService(
        MetricsService metricsService) {
      this.metricsService = metricsService;
    }

    public void setRepositoryChannel(
        TransactionRepositoryChannel repositoryChannel) {
      this.repositoryChannel = repositoryChannel;
    }
  }

  static class CleanMemForTest implements Runnable {

    final ConcurrentHashMap<String, SagaData> sagaDataMap;

    public CleanMemForTest(ConcurrentHashMap<String, SagaData> sagaDataMap) {
      this.sagaDataMap = sagaDataMap;
    }

    @Override
    public void run() {
      while (true) {
        try {
          sagaDataMap.clear();
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        } finally {
          try {
            Thread.sleep(10000);
          } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
          }
        }
      }
    }
  }
}
