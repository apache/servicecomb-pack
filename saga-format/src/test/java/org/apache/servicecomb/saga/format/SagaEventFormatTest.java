
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

package org.apache.servicecomb.saga.format;

import static org.apache.servicecomb.saga.core.Operation.TYPE_REST;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.apache.servicecomb.saga.core.JacksonToJsonFormat;
import org.apache.servicecomb.saga.core.SagaRequest;
import org.apache.servicecomb.saga.core.SagaRequestImpl;
import org.apache.servicecomb.saga.core.SagaResponse;
import org.apache.servicecomb.saga.core.SuccessfulSagaResponse;
import org.apache.servicecomb.saga.core.ToJsonFormat;
import org.apache.servicecomb.saga.core.TransactionCompensatedEvent;
import org.apache.servicecomb.saga.core.TransactionFailedException;
import org.apache.servicecomb.saga.core.TransactionStartedEvent;
import org.apache.servicecomb.saga.transports.TransportFactory;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.seanyinx.github.unit.scaffolding.Randomness;

import org.apache.servicecomb.saga.core.FailedSagaResponse;
import org.apache.servicecomb.saga.core.RestOperation;
import org.apache.servicecomb.saga.core.SagaEvent;
import org.apache.servicecomb.saga.core.TransactionAbortedEvent;
import org.apache.servicecomb.saga.core.TransactionEndedEvent;
import org.apache.servicecomb.saga.transports.RestTransport;

public class SagaEventFormatTest {

  private final String sagaId = Randomness.uniquify("sagaId");
  private final Map<String, Map<String, String>> EMPTY_MAP = Collections.<String, Map<String, String>>emptyMap();
  private final SagaRequest request = new SagaRequestImpl(
      sagaId,
      Randomness.uniquify("serviceName"),
      TYPE_REST,
      new JacksonRestTransaction("/rest/xxx", "POST", singletonMap("query", singletonMap("foo", "xxx"))),
      new JacksonRestCompensation("/rest/xxx", "DELETE", singletonMap("query", singletonMap("bar", "xxx"))),
      new JacksonRestFallback(TYPE_REST, "/rest/xxx", "PUT", EMPTY_MAP)
  );

  private final RestTransport restTransport = Mockito.mock(RestTransport.class);
  private final TransportFactory transportFactory = Mockito.mock(TransportFactory.class);
  private final SagaEventFormat toEventFormat = new JacksonSagaEventFormat(transportFactory);
  private final ToJsonFormat toJsonFormat = new JacksonToJsonFormat();
  private final SuccessfulSagaResponse response = new SuccessfulSagaResponse("a wonderful day");

  @Before
  public void setUp() throws Exception {
    when(transportFactory.restTransport()).thenReturn(restTransport);
  }

  @After
  public void tearDown() throws Exception {
    verify(transportFactory, times(3)).restTransport();
  }

  @Test
  public void transactionStartedEventCanBeSerializedAndDeserialized() throws JsonProcessingException {
    TransactionStartedEvent event = new TransactionStartedEvent(sagaId, request);
    String json = event.json(toJsonFormat);

    SagaEvent sagaEvent = toEventFormat
        .toSagaEvent(sagaId, event.getClass().getSimpleName(), json);

    assertThat(sagaEvent, instanceOf(TransactionStartedEvent.class));
    assertThat(sagaEvent.sagaId, is(sagaId));
    assertThat(sagaEvent.payload(), eqToRequest(request));
  }

  @Test
  public void transactionEndedEventCanBeSerializedAndDeserialized() throws JsonProcessingException {
    TransactionEndedEvent event = new TransactionEndedEvent(sagaId, request, response);
    String json = event.json(toJsonFormat);

    SagaEvent sagaEvent = toEventFormat
        .toSagaEvent(sagaId, event.getClass().getSimpleName(), json);

    assertThat(sagaEvent, instanceOf(TransactionEndedEvent.class));
    assertThat(sagaEvent.sagaId, is(sagaId));
    assertThat(sagaEvent.payload(), eqToRequest(request));
    assertThat(((TransactionEndedEvent) sagaEvent).response(), eqToResponse(response));
  }

  @Test
  public void TransactionAbortedEventCanBeSerializedAndDeserialized() throws JsonProcessingException {
    TransactionFailedException exception = new TransactionFailedException("oops");
    SagaEvent event = new TransactionAbortedEvent(sagaId, request, exception);
    String json = event.json(toJsonFormat);

    SagaEvent sagaEvent = toEventFormat
        .toSagaEvent(sagaId, event.getClass().getSimpleName(), json);

    assertThat(sagaEvent, instanceOf(TransactionAbortedEvent.class));
    assertThat(sagaEvent.sagaId, is(sagaId));
    assertThat(sagaEvent.payload(), eqToRequest(request));
    assertThat(((TransactionAbortedEvent) sagaEvent).response(), eqToResponse(new FailedSagaResponse(exception)));
  }

  @Test
  public void compensationEndedEventCanBeSerializedAndDeserialized() throws JsonProcessingException {
    TransactionCompensatedEvent event = new TransactionCompensatedEvent(sagaId, request, response);
    String json = event.json(toJsonFormat);

    SagaEvent sagaEvent = toEventFormat
        .toSagaEvent(sagaId, event.getClass().getSimpleName(), json);

    assertThat(sagaEvent, instanceOf(TransactionCompensatedEvent.class));
    assertThat(sagaEvent.sagaId, is(sagaId));
    assertThat(sagaEvent.payload(), eqToRequest(request));
    assertThat(((TransactionCompensatedEvent) sagaEvent).response(), eqToResponse(response));
  }

  private static Matcher<SagaRequest> eqToRequest(final SagaRequest expected) {
    return new TypeSafeMatcher<SagaRequest>() {
      @Override
      protected boolean matchesSafely(SagaRequest request) {
        return expected.id().equals(request.id())
            && request.serviceName().equals(expected.serviceName())
            && request.task().equals(expected.task())
            && request.type().equals(expected.type())
            && ((RestOperation) request.transaction()).path().equals(((RestOperation) expected.transaction()).path())
            && ((RestOperation) request.transaction()).method().equals(((RestOperation) expected.transaction()).method())
            && ((RestOperation) request.compensation()).path().equals(((RestOperation) expected.compensation()).path())
            && ((RestOperation) request.compensation()).method().equals(((RestOperation) expected.compensation()).method());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(expected.toString());
      }
    };

  }

  private static Matcher<SagaResponse> eqToResponse(final SagaResponse expected) {
    return new TypeSafeMatcher<SagaResponse>() {
      @Override
      protected boolean matchesSafely(SagaResponse response) {
        return expected.body().equals(response.body())
            && response.succeeded() == expected.succeeded();
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(expected.toString());
      }
    };
  }

}
