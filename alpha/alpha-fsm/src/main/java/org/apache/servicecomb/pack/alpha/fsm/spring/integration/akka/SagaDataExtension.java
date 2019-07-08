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
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension.SagaDataExt;

public class SagaDataExtension extends AbstractExtensionId<SagaDataExt> {

  public static final SagaDataExtension SAGA_DATA_EXTENSION_PROVIDER = new SagaDataExtension();

  @Override
  public SagaDataExt createExtension(ExtendedActorSystem system) {
    return new SagaDataExt();
  }

  public static class SagaDataExt implements Extension {
    private ConcurrentSkipListMap<String, SagaData> sagaDataMap = new ConcurrentSkipListMap();

    public void putSagaData(String globalTxId, SagaData sagaData){
      sagaDataMap.put(globalTxId, sagaData);
    }

    public SagaData getSagaData(String globalTxId){
      return sagaDataMap.get(globalTxId);
    }

    public void clearSagaData(){
      sagaDataMap.clear();
    }

    public SagaData getLastSagaData(){
      return sagaDataMap.lastEntry().getValue();
    }
  }
}
