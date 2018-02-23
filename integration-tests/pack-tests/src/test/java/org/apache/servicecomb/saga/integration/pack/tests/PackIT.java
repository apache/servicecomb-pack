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

import org.apache.servicecomb.saga.alpha.core.TxEvent;
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

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private OmegaContext omegaContext;

  @Autowired
  private TxEventEnvelopeRepository eventRepo;

  @Autowired
  private CommandEnvelopeRepository commandRepo;

  @Autowired
  private Queue<String> compensatedMessages;

  @Autowired
  private GreetingService greetingService;

  @After
  public void tearDown() throws Exception {
    eventRepo.deleteAll();
    commandRepo.deleteAll();
    compensatedMessages.clear();
    greetingService.resetCount();
  }

  @Test(timeout = 5000)
  public void updatesTxStateToAlpha() throws Exception {
    ResponseEntity<String> entity = restTemplate.getForEntity("/greet?name={name}",
        String.class,
        "mike");

    assertThat(entity.getStatusCode(), is(OK));
    assertThat(entity.getBody(), is("Greetings, mike; Bonjour, mike"));

    List<String> distinctGlobalTxIds = eventRepo.findDistinctGlobalTxId();
    assertThat(distinctGlobalTxIds.size(), is(1));

    String globalTxId = distinctGlobalTxIds.get(0);
    List<TxEvent> events = eventRepo.findByGlobalTxIdOrderByCreationTime(globalTxId);

    assertThat(events.size(), is(6));

    TxEvent sagaStartedEvent = events.get(0);
    assertThat(sagaStartedEvent.type(), is("SagaStartedEvent"));
    assertThat(sagaStartedEvent.localTxId(), is(globalTxId));
    assertThat(sagaStartedEvent.parentTxId(), is(nullValue()));
    assertThat(sagaStartedEvent.serviceName(), is(serviceName));
    assertThat(sagaStartedEvent.instanceId(), is(notNullValue()));

    TxEvent txStartedEvent1 = events.get(1);
    assertThat(txStartedEvent1.type(), is("TxStartedEvent"));
    assertThat(txStartedEvent1.localTxId(), is(notNullValue()));
    assertThat(txStartedEvent1.parentTxId(), is(globalTxId));
    assertThat(txStartedEvent1.serviceName(), is(serviceName));
    assertThat(txStartedEvent1.instanceId(), is(sagaStartedEvent.instanceId()));

    TxEvent txEndedEvent1 = events.get(2);
    assertThat(txEndedEvent1.type(), is("TxEndedEvent"));
    assertThat(txEndedEvent1.localTxId(), is(txStartedEvent1.localTxId()));
    assertThat(txEndedEvent1.parentTxId(), is(globalTxId));
    assertThat(txEndedEvent1.serviceName(), is(serviceName));
    assertThat(txEndedEvent1.instanceId(), is(txStartedEvent1.instanceId()));

    TxEvent txStartedEvent2 = events.get(3);
    assertThat(txStartedEvent2.type(), is("TxStartedEvent"));
    assertThat(txStartedEvent2.localTxId(), is(notNullValue()));
    assertThat(txStartedEvent2.parentTxId(), is(globalTxId));
    assertThat(txStartedEvent2.serviceName(), is(serviceName));
    assertThat(txStartedEvent2.instanceId(), is(notNullValue()));

    TxEvent txEndedEvent2 = events.get(4);
    assertThat(txEndedEvent2.type(), is("TxEndedEvent"));
    assertThat(txEndedEvent2.localTxId(), is(txStartedEvent2.localTxId()));
    assertThat(txEndedEvent2.parentTxId(), is(globalTxId));
    assertThat(txEndedEvent2.serviceName(), is(serviceName));
    assertThat(txEndedEvent2.instanceId(), is(txStartedEvent2.instanceId()));

    TxEvent sagaEndedEvent = events.get(5);
    assertThat(sagaEndedEvent.type(), is("SagaEndedEvent"));
    assertThat(sagaEndedEvent.localTxId(), is(globalTxId));
    assertThat(sagaEndedEvent.parentTxId(), is(nullValue()));
    assertThat(sagaEndedEvent.serviceName(), is(serviceName));
    assertThat(sagaEndedEvent.instanceId(), is(notNullValue()));

    assertThat(compensatedMessages.isEmpty(), is(true));
  }

  @Test(timeout = 5000)
  public void compensatesFailedGlobalTransaction() throws Exception {
    ResponseEntity<String> entity = restTemplate.getForEntity("/greet?name={name}",
        String.class,
        TRESPASSER);

    assertThat(entity.getStatusCode(), is(INTERNAL_SERVER_ERROR));

    await().atMost(2, SECONDS).until(() -> eventRepo.count() == 7);

    List<String> distinctGlobalTxIds = eventRepo.findDistinctGlobalTxId();
    assertThat(distinctGlobalTxIds.size(), is(1));

    String globalTxId = distinctGlobalTxIds.get(0);
    List<TxEvent> events = eventRepo.findByGlobalTxIdOrderByCreationTime(globalTxId);
    assertThat(events.size(), is(7));

    TxEvent sagaStartedEvent = events.get(0);
    assertThat(sagaStartedEvent.type(), is("SagaStartedEvent"));

    TxEvent txStartedEvent1 = events.get(1);
    assertThat(txStartedEvent1.type(), is("TxStartedEvent"));
    assertThat(events.get(2).type(), is("TxEndedEvent"));

    TxEvent txStartedEvent2 = events.get(3);
    assertThat(txStartedEvent2.type(), is("TxStartedEvent"));

    TxEvent txAbortedEvent = events.get(4);
    assertThat(txAbortedEvent.type(), is("TxAbortedEvent"));
    assertThat(txAbortedEvent.localTxId(), is(txStartedEvent2.localTxId()));
    assertThat(txAbortedEvent.parentTxId(), is(globalTxId));
    assertThat(txAbortedEvent.serviceName(), is(serviceName));
    assertThat(txAbortedEvent.instanceId(), is(txStartedEvent2.instanceId()));

    // TODO: 2018/1/9 compensation shall be done in reverse order
    TxEvent txCompensatedEvent1 = events.get(5);
    assertThat(txCompensatedEvent1.type(), is("TxCompensatedEvent"));
    assertThat(txCompensatedEvent1.localTxId(), is(txStartedEvent1.localTxId()));
    assertThat(txCompensatedEvent1.parentTxId(), is(globalTxId));
    assertThat(txCompensatedEvent1.serviceName(), is(serviceName));
    assertThat(txCompensatedEvent1.instanceId(), is(txStartedEvent1.instanceId()));

    assertThat(events.get(6).type(), is("SagaEndedEvent"));

    assertThat(compensatedMessages, contains("Goodbye, " + TRESPASSER));
  }

  @Test(timeout = 5000)
  public void updatesEmbeddedTxStateToAlpha() throws Exception {
    ResponseEntity<String> entity = restTemplate.getForEntity("/goodMorning?name={name}",
        String.class,
        "mike");

    assertThat(entity.getStatusCode(), is(OK));
    assertThat(entity.getBody(), is("Good morning, Bonjour, mike"));

    List<String> distinctGlobalTxIds = eventRepo.findDistinctGlobalTxId();
    assertThat(distinctGlobalTxIds.size(), is(1));

    String globalTxId = distinctGlobalTxIds.get(0);
    List<TxEvent> events = eventRepo.findByGlobalTxIdOrderByCreationTime(globalTxId);

    assertThat(events.size(), is(6));

    TxEvent sagaStartedEvent = events.get(0);
    assertThat(sagaStartedEvent.type(), is("SagaStartedEvent"));

    TxEvent txStartedEvent1 = events.get(1);
    assertThat(txStartedEvent1.type(), is("TxStartedEvent"));
    assertThat(txStartedEvent1.localTxId(), is(notNullValue()));
    assertThat(txStartedEvent1.parentTxId(), is(globalTxId));

    TxEvent txStartedEvent2 = events.get(2);
    assertThat(txStartedEvent2.type(), is("TxStartedEvent"));
    assertThat(txStartedEvent2.localTxId(), is(notNullValue()));
    assertThat(txStartedEvent2.parentTxId(), is(txStartedEvent1.localTxId()));

    TxEvent txEndedEvent2 = events.get(3);
    assertThat(txEndedEvent2.type(), is("TxEndedEvent"));
    assertThat(txEndedEvent2.localTxId(), is(txStartedEvent2.localTxId()));
    assertThat(txEndedEvent2.parentTxId(), is(txStartedEvent1.localTxId()));

    TxEvent txEndedEvent1 = events.get(4);
    assertThat(txEndedEvent1.type(), is("TxEndedEvent"));
    assertThat(txEndedEvent1.localTxId(), is(txStartedEvent1.localTxId()));
    assertThat(txEndedEvent1.parentTxId(), is(globalTxId));

    TxEvent sagaEndedEvent = events.get(5);
    assertThat(sagaEndedEvent.type(), is("SagaEndedEvent"));

    assertThat(compensatedMessages.isEmpty(), is(true));
  }

  @Test(timeout = 5000)
  public void retrySubTransactionSuccess() {
    ResponseEntity<String> entity = restTemplate.getForEntity("/open?name={name}&retries={retries}",
        String.class,
        "eric",
        2);

    assertThat(entity.getStatusCode(), is(OK));
    assertThat(entity.getBody(), is("Greetings, eric; Welcome to visit the zoo, eric"));

    await().atMost(3, SECONDS).until(() -> eventRepo.count() == 8);

    List<String> distinctGlobalTxIds = eventRepo.findDistinctGlobalTxId();
    assertThat(distinctGlobalTxIds.size(), is(1));

    String globalTxId = distinctGlobalTxIds.get(0);
    List<TxEvent> events = eventRepo.findByGlobalTxIdOrderByCreationTime(globalTxId);
    assertThat(events.size(), is(8));

    assertThat(events.get(0).type(), is("SagaStartedEvent"));
    assertThat(events.get(1).type(), is("TxStartedEvent"));
    assertThat(events.get(2).type(), is("TxEndedEvent"));
    assertThat(events.get(3).type(), is("TxStartedEvent"));
    assertThat(events.get(4).type(), is("TxAbortedEvent"));
    assertThat(events.get(5).type(), is("TxStartedEvent"));
    assertThat(events.get(6).type(), is("TxEndedEvent"));
    assertThat(events.get(7).type(), is("SagaEndedEvent"));

    assertThat(compensatedMessages.isEmpty(), is(true));
  }

  @Test(timeout = 5000)
  public void compensateWhenRetryReachesMaximum() throws InterruptedException {
    // retries 3 times and then compensate
    ResponseEntity<String> entity = restTemplate.getForEntity("/open?name={name}&retries={retries}",
        String.class,
        TRESPASSER,
        5);

    assertThat(entity.getStatusCode(), is(INTERNAL_SERVER_ERROR));

    await().atMost(3, SECONDS).until(() -> eventRepo.count() == 11);

    List<String> distinctGlobalTxIds = eventRepo.findDistinctGlobalTxId();
    assertThat(distinctGlobalTxIds.size(), is(1));

    String globalTxId = distinctGlobalTxIds.get(0);
    List<TxEvent> events = eventRepo.findByGlobalTxIdOrderByCreationTime(globalTxId);
    assertThat(events.size(), is(11));

    assertThat(events.get(0).type(), is("SagaStartedEvent"));
    assertThat(events.get(1).type(), is("TxStartedEvent"));
    assertThat(events.get(2).type(), is("TxEndedEvent"));
    assertThat(events.get(3).type(), is("TxStartedEvent"));
    assertThat(events.get(4).type(), is("TxAbortedEvent"));
    assertThat(events.get(5).type(), is("TxStartedEvent"));
    assertThat(events.get(6).type(), is("TxAbortedEvent"));
    assertThat(events.get(7).type(), is("TxStartedEvent"));
    assertThat(events.get(8).type(), is("TxAbortedEvent"));
    assertThat(events.get(9).type(), is("TxCompensatedEvent"));
    assertThat(events.get(10).type(), is("SagaEndedEvent"));

    assertThat(compensatedMessages, contains("Goodbye, " + TRESPASSER));
  }
}
