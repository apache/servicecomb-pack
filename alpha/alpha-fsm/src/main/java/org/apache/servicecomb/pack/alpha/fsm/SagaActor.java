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

package org.apache.servicecomb.pack.alpha.fsm;

import akka.actor.Props;
import akka.persistence.fsm.AbstractPersistentFSM;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaAbortedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaDomainEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaDomainEvent.DomainEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaTimeoutEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxComponsitedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.base.SagaEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.base.TxEvent;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.model.TxEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

public class SagaActor extends
    AbstractPersistentFSM<SagaActorState, SagaData, DomainEvent> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static Props props(String persistenceId) {
    return Props.create(SagaActor.class, persistenceId);
  }

  private final String persistenceId;

  public SagaActor(String persistenceId) {
    this.persistenceId = persistenceId;

    startWith(SagaActorState.IDEL, SagaData.builder().build());

    when(SagaActorState.IDEL,
        matchEvent(SagaStartedEvent.class,
            (event, data) -> {
              data.setGlobalTxId(event.getGlobalTxId());
              data.setBeginTime(System.currentTimeMillis());
              if (event.getTimeout() > 0) {
                data.setExpirationTime(data.getBeginTime() + event.getTimeout() * 1000);
                return goTo(SagaActorState.READY)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.READY);
              }
            }

        )
    );

    when(SagaActorState.READY,
        matchEvent(TxStartedEvent.class, SagaData.class,
            (event, data) -> {
              updateTxEntity(event, data);
              if (data.getExpirationTime() > 0) {
                return goTo(SagaActorState.PARTIALLY_ACTIVE)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.PARTIALLY_ACTIVE);
              }
            }
        ).event(SagaEndedEvent.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            }
        ).event(SagaAbortedEvent.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED)
                  .replying(data);
            })
    );

    when(SagaActorState.PARTIALLY_ACTIVE,
        matchEvent(TxEndedEvent.class, SagaData.class,
            (event, data) -> {
              updateTxEntity(event, data);
              if (data.getExpirationTime() > 0) {
                return goTo(SagaActorState.PARTIALLY_COMMITTED)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.PARTIALLY_COMMITTED);
              }
            }
        ).event(TxStartedEvent.class,
            (event, data) -> {
              updateTxEntity(event, data);
              if (data.getExpirationTime() > 0) {
                return stay()
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return stay();
              }
            }
        ).event(SagaTimeoutEvent.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(TxAbortedEvent.class,
            (event, data) -> {
              updateTxEntity(event, data);
              return goTo(SagaActorState.FAILED);
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            })
    );

    when(SagaActorState.PARTIALLY_COMMITTED,
        matchEvent(TxStartedEvent.class,
            (event, data) -> {
              updateTxEntity(event, data);
              if (data.getExpirationTime() > 0) {
                return goTo(SagaActorState.PARTIALLY_ACTIVE)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.PARTIALLY_ACTIVE);
              }
            }
        ).event(TxEndedEvent.class,
            (event, data) -> {
              updateTxEntity(event, data);
              if (data.getExpirationTime() > 0) {
                return stay()
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return stay();
              }
            }
        ).event(SagaTimeoutEvent.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(SagaEndedEvent.class,
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              return goTo(SagaActorState.COMMITTED)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(SagaAbortedEvent.class,
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              updateTxEntity(event, data);
              return goTo(SagaActorState.FAILED);
            }
        ).event(TxAbortedEvent.class,
            (event, data) -> {
              updateTxEntity(event, data);
              return goTo(SagaActorState.FAILED);
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            })
    );

    when(SagaActorState.FAILED,
        matchEvent(SagaTimeoutEvent.class, SagaData.class,
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              return goTo(SagaActorState.SUSPENDED)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(TxComponsitedEvent.class, SagaData.class,
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              updateTxEntity(event, data);
              if ((!data.isTerminated() && data.getCompensationRunningCounter().intValue() > 0)
                  || hasCommittedTx(data)) {
                return stay();
              } else {
                return goTo(SagaActorState.COMPENSATED)
                    .replying(data)
                    .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
              }
            }
        ).event(SagaAbortedEvent.class, SagaData.class,
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              updateTxEntity(event, data);
              data.setTerminated(true);
              if ((!data.isTerminated() && data.getCompensationRunningCounter().intValue() > 0)
                  || hasCommittedTx(data)) {
                return stay();
              }else{
                return goTo(SagaActorState.COMPENSATED)
                    .replying(data)
                    .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
              }
            }
        ).event(TxStartedEvent.class, SagaData.class,
            (event, data) -> {
              updateTxEntity(event, data);
              return stay();
            }
        ).event(TxEndedEvent.class, SagaData.class,
            (event, data) -> {
              updateTxEntity(event, data);
              TxEntity txEntity = data.getTxEntityMap().get(event.getLocalTxId());
              // TODO call compensate
              compensation(txEntity, data);
              return stay();
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            })
    );

    when(SagaActorState.COMMITTED,
        matchAnyEvent(
            (event, data) -> {
              return stop();
            }
        )
    );

    when(SagaActorState.SUSPENDED,
        matchAnyEvent(
            (event, data) -> {
              return stop();
            }
        )
    );

    when(SagaActorState.COMPENSATED,
        matchAnyEvent(
            (event, data) -> {
              return stop();
            }
        )
    );

    whenUnhandled(
        matchAnyEvent((event, data) -> {
          LOG.error("Unhandled event {}", event);
          return goTo(SagaActorState.SUSPENDED).replying(data);
        })
    );

    onTransition(
        matchState(null, null, (from, to) -> {
          LOG.info("transition {} {} -> {}", getSelf(), from, to);
        })
    );

  }

  @Override
  public void onRecoveryCompleted() {
    LOG.info("onRecoveryCompleted: {} {}", stateName(), stateData());
  }

  @Override
  public Class domainEventClass() {
    return SagaDomainEvent.DomainEvent.class;
  }


  @Override
  public String persistenceId() {
    return persistenceId;
  }

  @Override
  public SagaData applyEvent(DomainEvent domainEvent, SagaData currentData) {
    return currentData;
  }

  private void updateTxEntity(BaseEvent event, SagaData data) {
    if (event instanceof TxEvent) {
      TxEvent txEvent = (TxEvent) event;
      if (!data.getTxEntityMap().containsKey(txEvent.getLocalTxId())) {
        if (event instanceof TxStartedEvent) {
          TxEntity txEntity = TxEntity.builder()
              .localTxId(txEvent.getLocalTxId())
              .parentTxId(txEvent.getParentTxId())
              .state(TxState.ACTIVE)
              .build();
          data.getTxEntityMap().put(txEntity.getLocalTxId(), txEntity);
        }
      } else {
        TxEntity txEntity = data.getTxEntityMap().get(txEvent.getLocalTxId());
        if (event instanceof TxEndedEvent) {
          if (txEntity.getState() == TxState.ACTIVE) {
            txEntity.setEndTime(System.currentTimeMillis());
            txEntity.setState(TxState.COMMITTED);
          }
        } else if (event instanceof TxAbortedEvent) {
          if (txEntity.getState() == TxState.ACTIVE) {
            txEntity.setEndTime(System.currentTimeMillis());
            txEntity.setState(TxState.FAILED);
            data.getTxEntityMap().forEach((k, v) -> {
              if (v.getState() == TxState.COMMITTED) {
                // call compensate
                compensation(v, data);
              }
            });
          }
        } else if (event instanceof TxComponsitedEvent) {
          // decrement the compensation running counter by one
          data.getCompensationRunningCounter().decrementAndGet();
          txEntity.setState(TxState.COMPENSATED);
          LOG.info("compensation is completed {}",txEntity.getLocalTxId());
        }
      }
    } else if (event instanceof SagaEvent) {
      if (event instanceof SagaAbortedEvent) {
        data.getTxEntityMap().forEach((k, v) -> {
          if (v.getState() == TxState.COMMITTED) {
            // call compensate
            compensation(v, data);
          }
        });
      }
    }
  }

  private boolean hasCommittedTx(SagaData data) {
    return data.getTxEntityMap().entrySet().stream()
        .filter(map -> map.getValue().getState() == TxState.COMMITTED)
        .count() > 0;
  }

  private void compensation(TxEntity txEntity, SagaData data) {
    // increments the compensation running counter by one
    data.getCompensationRunningCounter().incrementAndGet();
    LOG.info("compensate {}", txEntity.getLocalTxId());
  }
}
