/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.omega.context;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

public class UniqueIdGeneratorTest {

  private final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  private Callable<String> task = new Callable<String>() {
    @Override
    public String call() throws Exception {
      return idGenerator.nextId();
    }
  };

  @Test
  public void nextIdIsUnique() throws InterruptedException {
    int nThreads = 10;
    List<Callable<String>> tasks = Collections.nCopies(nThreads, task);
    ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
    List<Future<String>> futures = executorService.invokeAll(tasks);

    Set<String> ids = new HashSet<>();
    for (Future<String> future: futures) {
      try {
        ids.add(future.get());
      } catch (ExecutionException e) {
        fail("unable to retrieve next id, " + e);
      }
    }
    assertThat(ids.size(), is(nThreads));
  }
}
