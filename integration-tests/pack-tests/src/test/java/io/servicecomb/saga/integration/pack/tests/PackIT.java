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

package io.servicecomb.saga.integration.pack.tests;

import static io.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import io.servicecomb.saga.omega.context.OmegaContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GreetingApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT,
    properties = {"server.port=8080"})
public class PackIT {
  private final String globalTxId = UUID.randomUUID().toString();

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private OmegaContext omegaContext;

  @Autowired
  private TxEventEnvelopeRepository repository;

  @Test
  public void updatesTxStateToAlpha() throws Exception {
    HttpHeaders headers = new HttpHeaders();

    headers.set(GLOBAL_TX_ID_KEY, globalTxId);

    ResponseEntity<String> entity = restTemplate.exchange("/greet?name={name}",
        GET,
        new HttpEntity<>(headers),
        String.class,
        "mike");

    assertThat(entity.getStatusCode(), is(OK));
    assertThat(entity.getBody(), is("Greetings, mike; Bonjour, mike"));

    List<TxEventEnvelope> envelopes = repository.findByGlobalTxIdOrderByCreationTime(globalTxId);

    assertThat(envelopes.size(), is(4));
    assertThat(envelopes.get(0).type(), is("TxStartedEvent"));
    assertThat(envelopes.get(0).parentTxId(), is(nullValue()));
    assertThat(envelopes.get(1).type(), is("TxEndedEvent"));
    assertThat(envelopes.get(1).parentTxId(), is(nullValue()));

    assertThat(envelopes.get(2).type(), is("TxStartedEvent"));
    assertThat(envelopes.get(2).parentTxId(), is(envelopes.get(0).localTxId()));
    assertThat(envelopes.get(3).type(), is("TxEndedEvent"));
    assertThat(envelopes.get(3).parentTxId(), is(envelopes.get(0).localTxId()));
  }
}
