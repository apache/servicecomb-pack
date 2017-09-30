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

package io.servicecomb.saga.core.dag;

import static com.seanyinx.github.unit.scaffolding.Randomness.nextId;
import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.function.Predicate;

import org.junit.Test;

public class EdgeTest {

  private boolean satisfied;

  private final Predicate<String> predicate = (any) -> satisfied;
  private final Node<String, String> parent = new Node<>(nextId(), uniquify("parent"));
  private final Node<String, String> child = new Node<>(nextId(), uniquify("child"));

  private final Edge<String, String> edge = new Edge<>(predicate, parent, child);

  @Test
  public void linksParentAndChildNodes() throws Exception {
    assertThat(edge.source(), is(parent));
    assertThat(edge.target(), is(child));
  }

  @Test
  public void satisfiedOnlyWhenPredicateTestsSuccessfully() throws Exception {
    satisfied = true;
    assertThat(edge.isSatisfied(null), is(satisfied));

    satisfied = false;
    assertThat(edge.isSatisfied(null), is(satisfied));
  }
}