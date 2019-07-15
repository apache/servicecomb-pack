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

package org.apache.servicecomb.pack.alpha.fsm.metrics;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsBean {

  private AtomicLong eventReceived = new AtomicLong();
  private AtomicLong eventAccepted = new AtomicLong();
  private AtomicLong eventRejected = new AtomicLong();
  private AtomicDouble eventAvgTime = new AtomicDouble();//milliseconds moving average
  private AtomicLong actorReceived = new AtomicLong();
  private AtomicLong actorAccepted = new AtomicLong();
  private AtomicLong actorRejected = new AtomicLong();
  private AtomicDouble actorAvgTime = new AtomicDouble();//milliseconds moving average
  private AtomicLong sagaBeginCounter = new AtomicLong();
  private AtomicLong sagaEndCounter = new AtomicLong();
  private AtomicDouble sagaAvgTime = new AtomicDouble();//milliseconds moving average
  private AtomicLong committed = new AtomicLong();
  private AtomicLong compensated = new AtomicLong();
  private AtomicLong suspended = new AtomicLong();

  public void doEventReceived() {
    eventReceived.incrementAndGet();
  }

  public void doEventAccepted() {
    eventAccepted.incrementAndGet();
  }

  public void doEventRejected() {
    eventReceived.decrementAndGet();
    eventRejected.incrementAndGet();
  }

  public void doEventAvgTime(long time) {
    if (eventAvgTime.get() == 0) {
      eventAvgTime.set(time);
    } else {
      eventAvgTime.set((eventAvgTime.get() + time) / 2);
    }
  }

  public void doActorReceived() {
    actorReceived.incrementAndGet();
  }

  public void doActorAccepted() {
    actorAccepted.incrementAndGet();
  }

  public void doActorRejected() {
    actorReceived.decrementAndGet();
    actorRejected.incrementAndGet();
  }

  public void doActorAvgTime(long time) {
    if (actorAvgTime.get() == 0) {
      actorAvgTime.set(time);
    } else {
      actorAvgTime.set((actorAvgTime.get() + time) / 2);
    }
  }

  public void doSagaBeginCounter() {
    sagaBeginCounter.incrementAndGet();
  }

  public void doSagaEndCounter() {
    sagaEndCounter.incrementAndGet();
  }

  public void doSagaAvgTime(long time) {
    if (sagaAvgTime.get() == 0) {
      sagaAvgTime.set(time);
    } else {
      sagaAvgTime.set((sagaAvgTime.get() + time) / 2);
    }
  }

  public void doCommitted() {
    committed.incrementAndGet();
  }

  public void doCompensated() {
    compensated.incrementAndGet();
  }

  public void doSuspended() {
    suspended.incrementAndGet();
  }

  public long getEventReceived() {
    return eventReceived.get();
  }

  public long getEventAccepted() {
    return eventAccepted.get();
  }

  public long getEventRejected() {
    return eventRejected.get();
  }

  public double getEventAvgTime() {
    return eventAvgTime.get();
  }

  public long getActorReceived() {
    return actorReceived.get();
  }

  public long getActorAccepted() {
    return actorAccepted.get();
  }

  public long getActorRejected() {
    return actorRejected.get();
  }

  public double getActorAvgTime() {
    return actorAvgTime.get();
  }

  public long getSagaBeginCounter() {
    return sagaBeginCounter.get();
  }

  public long getSagaEndCounter() {
    return sagaEndCounter.get();
  }

  public double getSagaAvgTime() {
    return sagaAvgTime.get();
  }

  public long getCommitted() {
    return committed.get();
  }

  public long getCompensated() {
    return compensated.get();
  }

  public long getSuspended() {
    return suspended.get();
  }
}
