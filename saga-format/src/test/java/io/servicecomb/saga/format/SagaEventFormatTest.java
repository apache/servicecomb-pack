
/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.saga.format;

import static io.servicecomb.saga.core.SagaRequest.TYPE_REST;
import static java.util.Collections.singletonMap;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.seanyinx.github.unit.scaffolding.Randomness;
import io.servicecomb.saga.core.CompensationEndedEvent;
import io.servicecomb.saga.core.CompensationStartedEvent;
import io.servicecomb.saga.core.FailedSagaResponse;
import io.servicecomb.saga.core.JacksonToJsonFormat;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaRequest;
import io.servicecomb.saga.core.SagaRequestImpl;
import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.core.SuccessfulSagaResponse;
import io.servicecomb.saga.core.ToJsonFormat;
import io.servicecomb.saga.core.TransactionAbortedEvent;
import io.servicecomb.saga.core.TransactionEndedEvent;
import io.servicecomb.saga.core.TransactionFailedException;
import io.servicecomb.saga.core.TransactionStartedEvent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

public class SagaEventFormatTest {

  private final String sagaId = Randomness.uniquify("sagaId");
  private final SagaRequest request = new SagaRequestImpl(
      sagaId,
      Randomness.uniquify("serviceName"),
      TYPE_REST,
      new JacksonRestTransaction("/rest/xxx", "POST", singletonMap("query", singletonMap("foo", "xxx"))),
      new JacksonRestCompensation("/rest/xxx", "DELETE", singletonMap("query", singletonMap("bar", "xxx")))
  );

  private final SagaEventFormat toEventFormat = new JacksonSagaEventFormat();
  private final ToJsonFormat toJsonFormat = new JacksonToJsonFormat();
  private final SuccessfulSagaResponse response = new SuccessfulSagaResponse(200, "a wonderful day");

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
  public void compensationStartedEventCanBeSerializedAndDeserialized() throws JsonProcessingException {
    CompensationStartedEvent event = new CompensationStartedEvent(sagaId, request);
    String json = event.json(toJsonFormat);

    SagaEvent sagaEvent = toEventFormat
        .toSagaEvent(sagaId, event.getClass().getSimpleName(), json);

    assertThat(sagaEvent, instanceOf(CompensationStartedEvent.class));
    assertThat(sagaEvent.sagaId, is(sagaId));
    assertThat(sagaEvent.payload(), eqToRequest(request));
  }

  @Test
  public void compensationEndedEventCanBeSerializedAndDeserialized() throws JsonProcessingException {
    CompensationEndedEvent event = new CompensationEndedEvent(sagaId, request, response);
    String json = event.json(toJsonFormat);

    SagaEvent sagaEvent = toEventFormat
        .toSagaEvent(sagaId, event.getClass().getSimpleName(), json);

    assertThat(sagaEvent, instanceOf(CompensationEndedEvent.class));
    assertThat(sagaEvent.sagaId, is(sagaId));
    assertThat(sagaEvent.payload(), eqToRequest(request));
    assertThat(((CompensationEndedEvent) sagaEvent).response(), eqToResponse(response));
  }

  private static Matcher<SagaRequest> eqToRequest(SagaRequest expected) {
    return new TypeSafeMatcher<SagaRequest>() {
      @Override
      protected boolean matchesSafely(SagaRequest request) {
        return expected.id().equals(request.id())
            && request.serviceName().equals(expected.serviceName())
            && request.task().equals(expected.task())
            && request.type().equals(expected.type())
            && request.transaction().path().equals(expected.transaction().path())
            && request.transaction().method().equals(expected.transaction().method())
            && request.compensation().path().equals(expected.compensation().path())
            && request.compensation().method().equals(expected.compensation().method());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(expected.toString());
      }
    };

  }

  private static Matcher<SagaResponse> eqToResponse(SagaResponse expected) {
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
