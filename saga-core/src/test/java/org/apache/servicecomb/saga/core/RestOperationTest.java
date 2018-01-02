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

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RestOperationTest {

  @Test
  public void blowsUpWhenGetMethodWithForm() {
    try {
      new RestOperation("blah", "GET", singletonMap("form", emptyMap()));
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("GET & DELETE request cannot enclose a body"));
    }
  }

  @Test
  public void blowsUpWhenGetMethodWithJson() {
    try {
      new RestOperation("blah", "GET", singletonMap("json", emptyMap()));
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("GET & DELETE request cannot enclose a body"));
    }
  }

  @Test
  public void blowsUpWhenDeleteMethodWithForm() {
    try {
      new RestOperation("blah", "DELETE", singletonMap("form", emptyMap()));
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("GET & DELETE request cannot enclose a body"));
    }
  }

  @Test
  public void blowsUpWhenDeleteMethodWithJson() {
    try {
      new RestOperation("blah", "DELETE", singletonMap("json", emptyMap()));
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("GET & DELETE request cannot enclose a body"));
    }
  }

  @Test
  public void blowsUpWhenMethodIsNotSupported() {
    try {
      new RestOperation("blah", "foo", emptyMap());
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Unsupported method foo"));
    }
  }

  @Test
  public void blowsUpWhenMethodIsNull() {
    try {
      new RestOperation("blah", null, emptyMap());
      expectFailing(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Unsupported method null"));
    }
  }
}
