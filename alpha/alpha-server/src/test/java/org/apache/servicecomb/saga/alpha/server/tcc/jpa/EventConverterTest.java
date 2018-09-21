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

package org.apache.servicecomb.saga.alpha.server.tcc.jpa;

import org.junit.Test;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;


public class EventConverterTest {

  @Test
  public void getMethodInfo() {
    assertThat(EventConverter.toMethodInfo("test1", "test2"), is("confirm=test1,cancel=test2"));
  }

  @Test
  public void getMethodName() {
    assertThat(EventConverter.getMethodName("confirm=text1,cancel=text2",true), is("text1"));
    assertThat(EventConverter.getMethodName("confirm=text1,cancel=text2",false), is("text2"));
  }
}
