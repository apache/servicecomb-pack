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

package io.servicecomb.saga.format;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static io.servicecomb.saga.core.Operation.SUCCESSFUL_SAGA_RESPONSE;
import static io.servicecomb.saga.core.SagaResponse.EMPTY_RESPONSE;
import static java.util.Collections.emptyMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.servicecomb.saga.core.SagaResponse;
import io.servicecomb.saga.transports.RestTransport;

@SuppressWarnings("unchecked")
public class JacksonRestOperationTest {

  private final String address = uniquify("address");
  private final String path = uniquify("path");
  private final String method = "PUT";
  private final Map<String, Map<String, String>> params = new HashMap<>();

  private final RestTransport transport = Mockito.mock(RestTransport.class);
  private final JacksonRestOperation restOperation = new JacksonRestOperation(path, method, params);

  @Before
  public void setUp() throws Exception {
    restOperation.with(() -> transport);
  }

  @Test
  public void appendsResponseToForm() throws Exception {
    ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    when(transport.with(eq(address), eq(path), eq(method), argumentCaptor.capture())).thenReturn(EMPTY_RESPONSE);

    SagaResponse response = restOperation.send(address, SUCCESSFUL_SAGA_RESPONSE);

    assertThat(response, is(EMPTY_RESPONSE));

    Map<String, Map<String, String>> updatedParams = argumentCaptor.getValue();
    assertThat(updatedParams.getOrDefault("form", emptyMap()).get("response"), is(SUCCESSFUL_SAGA_RESPONSE.body()));
    assertThat(params.isEmpty(), is(true));
  }
}
