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
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.junit.BeforeClass;
import org.junit.Test;

public class NativeMessageFormatTest extends MessageFormatTestBase {

  @BeforeClass
  public static void setUp() {
    format = new NativeMessageFormat();
  }

  @Test
  public void blowsUpWhenSerializeEmptyClass() {
    try {
      format.serialize(eventOf(new EmptyClass()));
      expectFailing(OmegaException.class);
    } catch (OmegaException e) {
      assertThat(e.getMessage(), startsWith("Unable to serialize event with global tx id"));
    }
  }
}
