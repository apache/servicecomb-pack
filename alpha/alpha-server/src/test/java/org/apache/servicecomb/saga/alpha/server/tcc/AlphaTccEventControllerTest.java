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

package org.apache.servicecomb.saga.alpha.server.tcc;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static java.util.Collections.singletonList;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxEventDBRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.TccTxType;
import org.apache.servicecomb.saga.alpha.server.tcc.service.TccTxEventRepository;
import org.apache.servicecomb.saga.common.TransactionStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(AlphaTccEventController.class)
@ActiveProfiles("test")
public class AlphaTccEventControllerTest {
  private final TccTxEvent someEvent = someEvent();

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TccTxEventRepository eventRepository;

  @Before
  public void setUp() throws Exception {
    when(eventRepository.findAll()).thenReturn(singletonList(someEvent));
  }

  @Test
  public void retrievesEventsFromRepo() throws Exception {
    mockMvc.perform(get("/tcc/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$.[0].globalTxId", is(someEvent.getGlobalTxId())))
        .andExpect(jsonPath("$.[0].localTxId", is(someEvent.getLocalTxId())))
        .andExpect(jsonPath("$.[0].serviceName", is(someEvent.getServiceName())))
        .andExpect(jsonPath("$.[0].instanceId", is(someEvent.getInstanceId())))
        .andExpect(jsonPath("$.[0].txType", is(someEvent.getTxType())))
        .andExpect(jsonPath("$.[0].status", is(someEvent.getStatus())));

  }

  private TccTxEvent someEvent() {
    return new TccTxEvent(
        uniquify("serviceName"),
        uniquify("instanceId"),
        uniquify("globalTxId"),
        uniquify("localTxId"),
        uniquify("parentTxId"),
        TccTxType.STARTED.name(),
        TransactionStatus.Succeed.name());
  }

}
