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

package org.apache.servicecomb.saga.core;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;

public class CompositeSagaResponseTest {

  private final SagaResponse response1 = Mockito.mock(SagaResponse.class);
  private final SagaResponse response2 = Mockito.mock(SagaResponse.class);

  private final SagaResponse compositeSagaResponse = new CompositeSagaResponse(asList(response1, response2));

  @Test
  public void succeededOnlyWhenAllAreSuccessful() throws Exception {
    when(response1.succeeded()).thenReturn(true);
    when(response2.succeeded()).thenReturn(true);

    assertThat(compositeSagaResponse.succeeded(), is(true));
  }

  @Test
  public void failedWhenAnyIsNotSuccessful() throws Exception {
    when(response1.succeeded()).thenReturn(true);
    when(response2.succeeded()).thenReturn(false);

    assertThat(compositeSagaResponse.succeeded(), is(false));
  }

  @Test
  public void bodyCombinesAllResponseBodies() throws Exception {
    when(response1.body()).thenReturn("{\n"
        + "  \"status\": 500,\n"
        + "  \"body\" : \"oops\"\n"
        + "}\n");

    when(response2.body()).thenReturn("{\n"
        + "  \"status\": 200,\n"
        + "  \"body\" : \"blah\"\n"
        + "}\n");

    assertThat(compositeSagaResponse.body(), is("[{\n"
        + "  \"status\": 500,\n"
        + "  \"body\" : \"oops\"\n"
        + "}\n"
        + ", {\n"
        + "  \"status\": 200,\n"
        + "  \"body\" : \"blah\"\n"
        + "}\n"
        + "]"));
  }
}
