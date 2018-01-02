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

package org.apache.servicecomb.saga.omega.format;

import static com.seanyinx.github.unit.scaffolding.AssertUtils.expectFailing;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.TxEvent;
import org.junit.Test;

public class NativeMessageFormatTest {

  private final NativeMessageFormat format = new NativeMessageFormat();

  @Test
  public void serializeObjectIntoBytes() throws Exception {
    byte[] bytes = format.serialize(eventOf("hello", "world"));

    Object[] message = format.deserialize(bytes);

    assertThat(asList(message), contains("hello", "world"));
  }

  @Test
  public void blowsUpWhenObjectIsNotSerializable() throws Exception {
    try {
      format.serialize(eventOf(new NotSerializable()));
      expectFailing(OmegaException.class);
    } catch (OmegaException e) {
      assertThat(e.getMessage(), startsWith("Unable to serialize event with global tx id"));
    }
  }

  @Test
  public void blowsUpWhenObjectIsNotDeserializable() throws Exception {
    try {
      format.deserialize(new byte[0]);
      expectFailing(OmegaException.class);
    } catch (OmegaException e) {
      assertThat(e.getMessage(), startsWith("Unable to deserialize message"));
    }
  }

  private TxEvent eventOf(Object... payloads) {
    return new TxEvent(null, null, null, null, payloads);
  }

  private static class NotSerializable {
  }
}
