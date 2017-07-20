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

import java.util.ArrayList;
import java.util.List;

public class SagaCoordinator {

  private final EventStore eventStore;
  private final SagaRequest[] requests;

  public SagaCoordinator(EventStore eventStore, SagaRequest... requests) {
    this.eventStore = eventStore;
    this.requests = requests;
  }

  public void run() {
    List<SagaEvent> events = new ArrayList<>(eventStore.size());
    for (SagaEvent event : eventStore) {
      events.add(event);
    }

    Saga saga = new Saga(new LongIdGenerator(), eventStore, requests);

    saga.play(events);
    saga.run();
  }
}
