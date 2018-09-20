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

package org.apache.servicecomb.saga.alpha.server.tcc.service;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxType;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.junit.Before;
import org.junit.Test;


public class MemoryEventRegistryTest {
  private MemoryTxEventRepository registry = new MemoryTxEventRepository();
  private String gloableTxId = uniquify("globalTxId");
  private String serviceName = uniquify("serviceName");
  private String instanceId = uniquify("instanceId");
  private String localTxId1 = uniquify("localTxId");
  private String localTxId2 = uniquify("localTxId");


  @Before
  public void setup() {
    registry.save(someTccTxEvent(localTxId1, TccTxType.STARTED));
    registry.save(someTccTxEvent(localTxId1, TccTxType.PARTICIPATED));
    registry.save(someTccTxEvent(localTxId2, TccTxType.PARTICIPATED));
  }

  private TccTxEvent someTccTxEvent(String localTxId, TccTxType tccTxType) {
    return new TccTxEvent(serviceName, instanceId, gloableTxId, localTxId, "", tccTxType.name(), "", TransactionStatus.Succeed.name());
  }

  @Test
  public void testFindByGlobalTxId() {
    Optional<List<TccTxEvent>> result = registry.findByGlobalTxId(gloableTxId);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().size(), is(3));

    result = registry.findByGlobalTxId("globalTx");
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testfindByGlobalTxIdAndTxType() {
    Optional<List<TccTxEvent>> result = registry.findByGlobalTxIdAndTxType(gloableTxId, TccTxType.PARTICIPATED);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().size(), is(2));

    result = registry.findByGlobalTxIdAndTxType(gloableTxId, TccTxType.STARTED);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().size(), is(1));

    result = registry.findByGlobalTxIdAndTxType("globalTx", TccTxType.ENDED);
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testfindByUniqueKey() {
    Optional<TccTxEvent> result = registry.findByUniqueKey(gloableTxId, localTxId1, TccTxType.PARTICIPATED);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getLocalTxId(), is(localTxId1));
    assertThat(result.get().getTxType(), is(TccTxType.PARTICIPATED.name()));

    result = registry.findByUniqueKey(gloableTxId, localTxId2, TccTxType.PARTICIPATED);
    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getLocalTxId(), is(localTxId2));
    assertThat(result.get().getTxType(), is(TccTxType.PARTICIPATED.name()));

    result = registry.findByUniqueKey(gloableTxId, localTxId1, TccTxType.ENDED);
    assertThat(result.isPresent(), is(false));
   
  }

  @Test
  public void testfindAll() {
    Iterable<TccTxEvent> result = registry.findAll();
    List<String> events = new ArrayList<>();
    // Just check the event type here
    for(TccTxEvent event : result) {
      events.add(event.getTxType());
    }
    assertThat(events, contains("STARTED", "PARTICIPATED", "PARTICIPATED"));
  }

  @Test
  public void testDeleteAll() {
    registry.deleteAll();
    Iterable<TccTxEvent> result = registry.findAll();
    assertThat(result.iterator().hasNext(), is(false));
  }

}
