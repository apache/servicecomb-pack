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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension.SagaDataExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SagaDataExtension extends AbstractExtensionId<SagaDataExt> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final SagaDataExtension SAGA_DATA_EXTENSION_PROVIDER = new SagaDataExtension();
  public static boolean autoCleanSagaDataMap = true; // Only for Test

  @Override
  public SagaDataExt createExtension(ExtendedActorSystem system) {
    return new SagaDataExt();
  }

  public static class SagaDataExt implements Extension {
    //private final ConcurrentLinkedQueue<String> globalTxIds = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, SagaData> sagaDataMap = new ConcurrentHashMap();
    private String lastGlobalTxId;
    private CleanMemForTest cleanMemForTest = new CleanMemForTest(sagaDataMap);

    public SagaDataExt() {
      // Just to avoid the overflow of the OldGen for stress testing
      // Delete after SagaData persistence
      if(autoCleanSagaDataMap){
        new Thread(cleanMemForTest).start();
      }
    }

    public void putSagaData(String globalTxId, SagaData sagaData) {
      //if(!globalTxIds.contains(globalTxId)){
        lastGlobalTxId = globalTxId;
      //  globalTxIds.add(globalTxId);
      //}
      sagaDataMap.put(globalTxId, sagaData);
    }

    public void stopSagaData(String globalTxId, SagaData sagaData) {
      // TODO save SagaDate to database and clean sagaDataMap
      this.putSagaData(globalTxId, sagaData);
      lastGlobalTxId = globalTxId;
    }

    public SagaData getSagaData(String globalTxId) {
      // TODO If globalTxId does not exist in sagaDataMap then
      //  load from the database
      return sagaDataMap.get(globalTxId);
    }

    // Only test
    public void clearSagaData() {
      //globalTxIds.clear();
      lastGlobalTxId = null;
      sagaDataMap.clear();
    }

    public SagaData getLastSagaData() {
      return getSagaData(lastGlobalTxId);
    }
  }

  static class CleanMemForTest implements Runnable {
    final ConcurrentHashMap<String, SagaData> sagaDataMap;

    public CleanMemForTest(ConcurrentHashMap<String, SagaData> sagaDataMap) {
      this.sagaDataMap = sagaDataMap;
    }

    @Override
    public void run() {
      while (true){
        try{
          sagaDataMap.clear();
        }catch (Exception e){
          LOG.error(e.getMessage(),e);
        }finally {
          try {
            Thread.sleep(10000);
          } catch (InterruptedException e) {
            LOG.error(e.getMessage(),e);
          }
        }
      }
    }
  }
}
