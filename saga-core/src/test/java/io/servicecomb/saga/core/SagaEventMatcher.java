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

package io.servicecomb.saga.core;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

class SagaEventMatcher extends TypeSafeMatcher<EventEnvelope> {

  private final long id;
  private final Operation operation;
  private final Class<?> aClass;

  static Matcher<EventEnvelope> eventWith(long id, Operation operation, Class<?> aClass) {
    return new SagaEventMatcher(id, operation, aClass);
  }

  private SagaEventMatcher(long id, Operation operation, Class<?> aClass) {
    this.id = id;
    this.operation = operation;
    this.aClass = aClass;
  }

  @Override
  protected void describeMismatchSafely(EventEnvelope item, Description description) {
    description
        .appendText("EventEnvelope {id=" + item.id + ", operation=" + operation(item) + ", class=" + item.event.getClass());
  }

  @Override
  protected boolean matchesSafely(EventEnvelope envelope) {
    return envelope.id == id
        && operation(envelope).equals(operation)
        && envelope.event.getClass().equals(aClass);
  }

  @Override
  public void describeTo(Description description) {
    description
        .appendText("EventEnvelope {id=" + id + ", operation=" + operation + ", class=" + aClass.getCanonicalName());
  }

  private Operation operation(EventEnvelope envelope) {
    return operation instanceof Compensation ? envelope.event.payload().compensation() : envelope.event.payload().transaction();
  }
}
