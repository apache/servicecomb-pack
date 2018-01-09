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

package org.apache.servicecomb.saga.integration.pack.tests;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.servicecomb.saga.integration.pack.tests.GreetingController.TRESPASSER;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import java.util.List;
import java.util.Queue;
import java.util.UUID;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = GreetingApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT,
    properties = {"server.port=8080", "spring.application.name=greeting-service"})
public class PackIT {
  private static final String serviceName = "greeting-service";
  private final String globalTxId = UUID.randomUUID().toString();

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private OmegaContext omegaContext;

  @Autowired
  private TxEventEnvelopeRepository repository;

  @Autowired
  private Queue<String> compensatedMessages;

  @After
  public void tearDown() throws Exception {
    repository.deleteAll();
  }

  @Test
  public void updatesTxStateToAlpha() throws Exception {
    ResponseEntity<String> entity = restTemplate.getForEntity("/greet?name={name}",
        String.class,
        "mike");

    assertThat(entity.getStatusCode(), is(OK));
    assertThat(entity.getBody(), is("Greetings, mike; Bonjour, mike"));

    List<String> distinctGlobalTxIds = repository.findDistinctGlobalTxId();
    assertThat(distinctGlobalTxIds.size(), is(1));

    String globalTxId = distinctGlobalTxIds.get(0);
    List<TxEventEnvelope> envelopes = repository.findByGlobalTxIdOrderByCreationTime(globalTxId);

    assertThat(envelopes.size(), is(6));

    TxEventEnvelope sagaStartedEventEnvelope = envelopes.get(0);
    assertThat(sagaStartedEventEnvelope.type(), is("SagaStartedEvent"));
    assertThat(sagaStartedEventEnvelope.localTxId(), is(notNullValue()));
    assertThat(sagaStartedEventEnvelope.parentTxId(), is(nullValue()));
    assertThat(sagaStartedEventEnvelope.serviceName(), is(serviceName));
    assertThat(sagaStartedEventEnvelope.instanceId(), is(notNullValue()));

    TxEventEnvelope txStartedEventEnvelope1 = envelopes.get(1);
    assertThat(txStartedEventEnvelope1.type(), is("TxStartedEvent"));
    assertThat(txStartedEventEnvelope1.localTxId(), is(notNullValue()));
    assertThat(txStartedEventEnvelope1.parentTxId(), is(sagaStartedEventEnvelope.localTxId()));
    assertThat(txStartedEventEnvelope1.serviceName(), is(serviceName));
    assertThat(txStartedEventEnvelope1.instanceId(), is(sagaStartedEventEnvelope.instanceId()));

    TxEventEnvelope txEndedEventEnvelope1 = envelopes.get(2);
    assertThat(txEndedEventEnvelope1.type(), is("TxEndedEvent"));
    assertThat(txEndedEventEnvelope1.localTxId(), is(txStartedEventEnvelope1.localTxId()));
    assertThat(txEndedEventEnvelope1.parentTxId(), is(sagaStartedEventEnvelope.localTxId()));
    assertThat(txEndedEventEnvelope1.serviceName(), is(serviceName));
    assertThat(txEndedEventEnvelope1.instanceId(), is(txStartedEventEnvelope1.instanceId()));

    TxEventEnvelope txStartedEventEnvelope2 = envelopes.get(3);
    assertThat(txStartedEventEnvelope2.type(), is("TxStartedEvent"));
    assertThat(txStartedEventEnvelope2.localTxId(), is(notNullValue()));
    assertThat(txStartedEventEnvelope2.parentTxId(), is(txStartedEventEnvelope1.localTxId()));
    assertThat(txStartedEventEnvelope2.serviceName(), is(serviceName));
    assertThat(txStartedEventEnvelope2.instanceId(), is(notNullValue()));

    TxEventEnvelope txEndedEventEnvelope2 = envelopes.get(4);
    assertThat(txEndedEventEnvelope2.type(), is("TxEndedEvent"));
    assertThat(txEndedEventEnvelope2.localTxId(), is(txStartedEventEnvelope2.localTxId()));
    assertThat(txEndedEventEnvelope2.parentTxId(), is(txStartedEventEnvelope1.localTxId()));
    assertThat(txEndedEventEnvelope2.serviceName(), is(serviceName));
    assertThat(txEndedEventEnvelope2.instanceId(), is(txStartedEventEnvelope2.instanceId()));

    TxEventEnvelope sagaEndedEventEnvelope = envelopes.get(5);
    assertThat(sagaEndedEventEnvelope.type(), is("SagaEndedEvent"));
    assertThat(sagaEndedEventEnvelope.localTxId(), is(sagaStartedEventEnvelope.localTxId()));
    assertThat(sagaEndedEventEnvelope.parentTxId(), is(nullValue()));
    assertThat(sagaEndedEventEnvelope.serviceName(), is(serviceName));
    assertThat(sagaEndedEventEnvelope.instanceId(), is(notNullValue()));

    assertThat(compensatedMessages.isEmpty(), is(true));
  }

  @Test
  public void compensatesFailedGlobalTransaction() throws Exception {
    ResponseEntity<String> entity = restTemplate.getForEntity("/greet?name={name}",
        String.class,
        TRESPASSER);

    assertThat(entity.getStatusCode(), is(INTERNAL_SERVER_ERROR));

    await().atMost(2, SECONDS).until(() -> repository.count() == 8);

    List<String> distinctGlobalTxIds = repository.findDistinctGlobalTxId();
    assertThat(distinctGlobalTxIds.size(), is(1));

    String globalTxId = distinctGlobalTxIds.get(0);
    List<TxEventEnvelope> envelopes = repository.findByGlobalTxIdOrderByCreationTime(globalTxId);
    assertThat(envelopes.size(), is(8));

    TxEventEnvelope sagaStartedEventEnvelope = envelopes.get(0);
    assertThat(sagaStartedEventEnvelope.type(), is("SagaStartedEvent"));

    TxEventEnvelope txStartedEventEnvelope1 = envelopes.get(1);
    assertThat(txStartedEventEnvelope1.type(), is("TxStartedEvent"));
    assertThat(envelopes.get(2).type(), is("TxEndedEvent"));

    TxEventEnvelope txStartedEventEnvelope2 = envelopes.get(3);
    assertThat(txStartedEventEnvelope2.type(), is("TxStartedEvent"));

    TxEventEnvelope txAbortedEventEnvelope = envelopes.get(4);
    assertThat(txAbortedEventEnvelope.type(), is("TxAbortedEvent"));
    assertThat(txAbortedEventEnvelope.localTxId(), is(txStartedEventEnvelope2.localTxId()));
    assertThat(txAbortedEventEnvelope.parentTxId(), is(txStartedEventEnvelope1.localTxId()));
    assertThat(txAbortedEventEnvelope.serviceName(), is(serviceName));
    assertThat(txAbortedEventEnvelope.instanceId(), is(txStartedEventEnvelope2.instanceId()));

    TxEventEnvelope txCompensatedEventEnvelope1 = envelopes.get(5);
    assertThat(txCompensatedEventEnvelope1.type(), is("TxCompensatedEvent"));
    assertThat(txCompensatedEventEnvelope1.localTxId(), is(txStartedEventEnvelope1.localTxId()));
    assertThat(txCompensatedEventEnvelope1.parentTxId(), is(sagaStartedEventEnvelope.localTxId()));
    assertThat(txCompensatedEventEnvelope1.serviceName(), is(serviceName));
    assertThat(txCompensatedEventEnvelope1.instanceId(), is(txStartedEventEnvelope1.instanceId()));

    TxEventEnvelope txCompensatedEventEnvelope2 = envelopes.get(6);
    assertThat(txCompensatedEventEnvelope2.type(), is("TxCompensatedEvent"));
    assertThat(txCompensatedEventEnvelope2.localTxId(), is(txStartedEventEnvelope2.localTxId()));
    assertThat(txCompensatedEventEnvelope2.parentTxId(), is(txStartedEventEnvelope1.localTxId()));
    assertThat(txCompensatedEventEnvelope2.serviceName(), is(serviceName));
    assertThat(txCompensatedEventEnvelope2.instanceId(), is(txStartedEventEnvelope2.instanceId()));

    assertThat(envelopes.get(7).type(), is("SagaEndedEvent"));

    assertThat(compensatedMessages, contains(
        "Goodbye, " + TRESPASSER,
        "My bad, please take the window instead, " + TRESPASSER));
  }
}
