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

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.apache.servicecomb.saga.core.SagaResponse;
import org.junit.Test;

import org.apache.servicecomb.saga.core.SagaException;

public class ChildrenExtractorTest {

  private static final String json = "{\n"
      + "  \"foo\": \"bar\",\n"
      + "  \"sagaChildren\": [\n"
      + "    \"id1\",\n"
      + "    \"id2\",\n"
      + "    \"id3\"\n"
      + "  ]\n"
      + "}";

  private final ChildrenExtractor extractor = new ChildrenExtractor();

  @Test
  public void extractsChildrenIdFromJson() throws Exception {
    Set<String> children = extractor.fromJson(json);

    assertThat(children, containsInAnyOrder("id1", "id2", "id3"));
  }

  @Test
  public void emptyChildrenIfNoSuchField() throws Exception {
    Set<String> children = extractor.fromJson("{}");

    assertThat(children.isEmpty(), is(true));
  }

  @Test
  public void emptyChildrenIfNoneResponse() throws Exception {
    Set<String> children = extractor.fromJson(SagaResponse.NONE_RESPONSE.body());

    assertThat(children, containsInAnyOrder("none"));
  }

  @Test
  public void blowsUpWithInvalidJson() throws Exception {
    try {
      extractor.fromJson("blah");
      expectFailing(SagaException.class);
    } catch (SagaException e) {
      assertThat(e.getMessage(), is("Failed to deserialize json blah"));
    }
  }
}
