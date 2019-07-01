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
import org.apache.servicecomb.pack.alpha.fsm.domain.AddTxEventDomain;
import org.apache.servicecomb.pack.alpha.fsm.domain.DomainEvent;
import org.apache.servicecomb.pack.alpha.fsm.domain.SagaEndedDomain;
import org.apache.servicecomb.pack.alpha.fsm.domain.SagaStartedDomain;
import org.apache.servicecomb.pack.alpha.fsm.domain.UpdateTxEventDomain;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaAbortedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.SagaTimeoutEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxComponsitedCheckInternalEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxComponsitedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.model.TxEntity;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.LogExtension;
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
              SagaStartedDomain domainEvent = new SagaStartedDomain(event.getGlobalTxId(),
                  event.getCreateTime(), event.getTimeout());
              if (event.getTimeout() > 0) {
                return goTo(SagaActorState.READY)
                    .applying(domainEvent)
                    .forMax(Duration.create(event.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.READY)
                    .applying(domainEvent);
              }
            }

        )
    );

    when(SagaActorState.READY,
        matchEvent(TxStartedEvent.class, SagaData.class,
            (event, data) -> {
              AddTxEventDomain domainEvent = new AddTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId());
              if (data.getExpirationTime() > 0) {
                return goTo(SagaActorState.PARTIALLY_ACTIVE)
                    .applying(domainEvent)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.PARTIALLY_ACTIVE)
                    .applying(domainEvent);
              }
            }
        ).event(SagaEndedEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.SUSPENDED);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent)
                  .replying(data);
            }
        ).event(SagaAbortedEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.SUSPENDED);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent)
                  .replying(data);
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.SUSPENDED);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent)
                  .replying(data);
            })
    );

    when(SagaActorState.PARTIALLY_ACTIVE,
        matchEvent(TxEndedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId(), TxState.COMMITTED);
              if (data.getExpirationTime() > 0) {
                return goTo(SagaActorState.PARTIALLY_COMMITTED)
                    .applying(domainEvent)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.PARTIALLY_COMMITTED)
                    .applying(domainEvent);
              }
            }
        ).event(TxStartedEvent.class,
            (event, data) -> {
              AddTxEventDomain domainEvent = new AddTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId());
              if (data.getExpirationTime() > 0) {
                return stay()
                    .applying(domainEvent)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return stay().applying(domainEvent);
              }
            }
        ).event(SagaTimeoutEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.SUSPENDED);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(TxAbortedEvent.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId(), TxState.FAILED);
              return goTo(SagaActorState.FAILED)
                  .applying(domainEvent);
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            })
    );

    when(SagaActorState.PARTIALLY_COMMITTED,
        matchEvent(TxStartedEvent.class,
            (event, data) -> {
              AddTxEventDomain domainEvent = new AddTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId());
              if (data.getExpirationTime() > 0) {
                return goTo(SagaActorState.PARTIALLY_ACTIVE)
                    .applying(domainEvent)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return goTo(SagaActorState.PARTIALLY_ACTIVE)
                    .applying(domainEvent);
              }
            }
        ).event(TxEndedEvent.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId(), TxState.COMMITTED);
              if (data.getExpirationTime() > 0) {
                return stay()
                    .applying(domainEvent)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return stay().applying(domainEvent);
              }
            }
        ).event(SagaTimeoutEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.SUSPENDED);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(SagaEndedEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.COMMITTED);
              return goTo(SagaActorState.COMMITTED)
                  .applying(domainEvent)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(SagaAbortedEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.FAILED);
              return goTo(SagaActorState.FAILED).applying(domainEvent);
            }
        ).event(TxAbortedEvent.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId(), TxState.FAILED);
              return goTo(SagaActorState.FAILED).applying(domainEvent);
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            })
    );

    when(SagaActorState.FAILED,
        matchEvent(SagaTimeoutEvent.class, SagaData.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.SUSPENDED);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent)
                  .replying(data)
                  .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
            }
        ).event(TxComponsitedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId(), TxState.COMPENSATED);
              return stay().applying(domainEvent).andThen(exec(_data -> {
                self().tell(TxComponsitedCheckInternalEvent.builder().build(), self());
              }));
            }
        ).event(TxComponsitedCheckInternalEvent.class, SagaData.class,
            (event, data) -> {
              if ((!data.isTerminated() && data.getCompensationRunningCounter().intValue() > 0)
                  || hasCommittedTx(data)) {
                return stay().replying(data);
              } else {
                return goTo(SagaActorState.COMPENSATED)
                    .replying(data)
                    .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
              }
            }
        ).event(SagaAbortedEvent.class, SagaData.class,
            (event, data) -> {
              data.setTerminated(true);
              if (hasCommittedTx(data)) {
                SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.FAILED);
                return stay().replying(data).applying(domainEvent);
              } else if(hasCompensationSentTx(data)){
                return stay().replying(data);
              } else {
                SagaEndedDomain domainEvent = new SagaEndedDomain(
                    SagaActorState.COMPENSATED);
                return goTo(SagaActorState.COMPENSATED)
                    .applying(domainEvent)
                    .replying(data)
                    .forMax(Duration.create(1, TimeUnit.MILLISECONDS));
              }
            }
        ).event(TxStartedEvent.class, SagaData.class,
            (event, data) -> {
              AddTxEventDomain domainEvent = new AddTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId());
              return stay().applying(domainEvent);
            }
        ).event(TxEndedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event.getParentTxId(),
                  event.getLocalTxId(), TxState.COMMITTED);
              return stay().applying(domainEvent).andThen(exec(_data -> {
                TxEntity txEntity = _data.getTxEntityMap().get(event.getLocalTxId());
                // call compensate
                compensation(txEntity, _data);
              }));
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              return goTo(SagaActorState.SUSPENDED).replying(data);
            })
    );

    when(SagaActorState.COMMITTED,
        matchAnyEvent(
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              /**
               * deleteMessages 只会删除redis中actor的数据，但是不会删除actor的highestSequenceNr https://github.com/akka/akka/issues/21181
               * 已停止的 actor highestSequenceNr 需要手动清理，例如 actor 的持久化ID为 3c500008-7b9f-415f-b2fd-e6ad0d455fc1
               * 在Redis中当key=journal:persisted:3c500008-7b9f-415f-b2fd-e6ad0d455fc1:highestSequenceNr没有匹配的
               * key=journal:persisted:3c500008-7b9f-415f-b2fd-e6ad0d455fc1时，表示这个actor已经停止，可以使用以下命令清理
               * del journal:persisted:3c500008-7b9f-415f-b2fd-e6ad0d455fc1:highestSequenceNr
               * srem journal:persistenceIds 3c500008-7b9f-415f-b2fd-e6ad0d455fc1
               * */
              deleteMessages(lastSequenceNr());
              deleteSnapshot(snapshotSequenceNr());
              return stop();
            }
        )
    );

    when(SagaActorState.SUSPENDED,
        matchAnyEvent(
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              deleteMessages(lastSequenceNr());
              deleteSnapshot(snapshotSequenceNr());
              return stop();
            }
        )
    );

    when(SagaActorState.COMPENSATED,
        matchAnyEvent(
            (event, data) -> {
              data.setEndTime(System.currentTimeMillis());
              deleteMessages(lastSequenceNr());
              deleteSnapshot(snapshotSequenceNr());
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
          if (stateData().getGlobalTxId() != null) {
            stateData().setLastState(to);
            LogExtension.LogExtensionProvider.get(getContext().getSystem())
                .putSagaData(stateData().getGlobalTxId(), stateData());
          }
          LOG.info("transition {} {} -> {}", getSelf(), from, to);
        })
    );

    onTermination(
        matchStop(
            Normal(), (state, data) -> {
              LOG.info("stop {} {}", data.getGlobalTxId(), state);
              data.setTerminated(true);
              data.setLastState(state);
              LogExtension.LogExtensionProvider.get(getContext().getSystem())
                  .putSagaData(data.getGlobalTxId(), data);
            }
        )
    );

  }

  @Override
  public SagaData applyEvent(DomainEvent event, SagaData data) {
    if (event instanceof SagaStartedDomain) {
      SagaStartedDomain domainEvent = (SagaStartedDomain) event;
      data.setGlobalTxId(domainEvent.getGlobalTxId());
      data.setBeginTime(domainEvent.getCreateTime());
      data.setExpirationTime(domainEvent.getExpirationTime());
    } else if (event instanceof AddTxEventDomain) {
      AddTxEventDomain domainEvent = (AddTxEventDomain) event;
      if (!data.getTxEntityMap().containsKey(domainEvent.getLocalTxId())) {
        TxEntity txEntity = TxEntity.builder()
            .localTxId(domainEvent.getLocalTxId())
            .parentTxId(domainEvent.getParentTxId())
            .state(domainEvent.getState())
            .build();
        data.getTxEntityMap().put(txEntity.getLocalTxId(), txEntity);
      } else {
        LOG.warn("TxEntity {} already exists", domainEvent.getLocalTxId());
      }
    } else if (event instanceof UpdateTxEventDomain) {
      UpdateTxEventDomain domainEvent = (UpdateTxEventDomain) event;
      TxEntity txEntity = data.getTxEntityMap().get(domainEvent.getLocalTxId());
      txEntity.setEndTime(System.currentTimeMillis());
      if (domainEvent.getState() == TxState.COMMITTED) {
        txEntity.setState(domainEvent.getState());
      } else if (domainEvent.getState() == TxState.FAILED) {
        txEntity.setState(domainEvent.getState());
        data.getTxEntityMap().forEach((k, v) -> {
          if (v.getState() == TxState.COMMITTED) {
            // call compensate
            compensation(v, data);
          }
        });
      } else if (domainEvent.getState() == TxState.COMPENSATED) {
        // decrement the compensation running counter by one
        data.getCompensationRunningCounter().decrementAndGet();
        txEntity.setState(domainEvent.getState());
        LOG.info("compensation is completed {}", txEntity.getLocalTxId());
      }
    } else if (event instanceof SagaEndedDomain) {
      SagaEndedDomain domainEvent = (SagaEndedDomain) event;
      if (domainEvent.getState() == SagaActorState.FAILED) {
        data.getTxEntityMap().forEach((k, v) -> {
          if (v.getState() == TxState.COMMITTED) {
            // call compensate
            compensation(v, data);
          }
        });
      } else if (domainEvent.getState() == SagaActorState.SUSPENDED) {

      } else if (domainEvent.getState() == SagaActorState.COMPENSATED) {

      }
    }
    LOG.debug("applyEvent: {} {}", stateName(), stateData().getGlobalTxId());
    return data;
  }

  @Override
  public void onRecoveryCompleted() {
    LOG.debug("onRecoveryCompleted: {} {}", stateName(), stateData().getGlobalTxId());
  }

  @Override
  public Class domainEventClass() {
    return DomainEvent.class;
  }


  @Override
  public String persistenceId() {
    return persistenceId;
  }

  private boolean hasCommittedTx(SagaData data) {
    return data.getTxEntityMap().entrySet().stream()
        .filter(map -> map.getValue().getState() == TxState.COMMITTED)
        .count() > 0;
  }

  private boolean hasCompensationSentTx(SagaData data) {
    return data.getTxEntityMap().entrySet().stream()
        .filter(map -> map.getValue().getState() == TxState.COMPENSATION_SENT)
        .count() > 0;
  }

  private void compensation(TxEntity txEntity, SagaData data) {
    // increments the compensation running counter by one
    data.getCompensationRunningCounter().incrementAndGet();
    //TODO call omega compensate method
    LOG.info("compensate {}", txEntity.getLocalTxId());
    txEntity.setState(TxState.COMPENSATION_SENT);
  }
}
