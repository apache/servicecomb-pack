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

package io.servicecomb.saga.core.application;

import io.servicecomb.saga.core.PersistentLog;
import io.servicecomb.saga.core.SagaEvent;
import io.servicecomb.saga.core.SagaLog;
import kamon.annotation.EnableKamon;
import kamon.annotation.Segment;

@EnableKamon
class CompositeSagaLog implements SagaLog {

  private final SagaLog embedded;
  private final PersistentLog persistent;

  CompositeSagaLog(SagaLog embedded, PersistentLog persistent) {
    this.embedded = embedded;
    this.persistent = persistent;
  }

  @Segment(name = "compositeSagaLog", category = "application", library = "kamon")
  @Override
  public void offer(SagaEvent sagaEvent) {
    persistent.offer(sagaEvent);
    embedded.offer(sagaEvent);
  }
}
