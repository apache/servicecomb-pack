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
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.servicecomb.pack.alpha.core.AlphaException;
import org.apache.servicecomb.pack.alpha.core.fsm.SuspendedType;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.fsm.domain.AddTxEventDomain;
import org.apache.servicecomb.pack.alpha.fsm.domain.DomainEvent;
import org.apache.servicecomb.pack.alpha.fsm.domain.SagaEndedDomain;
import org.apache.servicecomb.pack.alpha.fsm.domain.SagaStartedDomain;
import org.apache.servicecomb.pack.alpha.fsm.domain.UpdateTxEventDomain;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaTimeoutEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensatedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.internal.ComponsitedCheckEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.fsm.model.SagaData;
import org.apache.servicecomb.pack.alpha.fsm.model.TxEntity;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SagaDataExtension;
import org.apache.servicecomb.pack.alpha.fsm.spring.integration.akka.SpringAkkaExtension;
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

  private long sagaBeginTime;
  private long sagaEndTime;

  public SagaActor(String persistenceId) {
    this.persistenceId = persistenceId;

    startWith(SagaActorState.IDLE, SagaData.builder().build());

    when(SagaActorState.IDLE,
        matchEvent(SagaStartedEvent.class,
            (event, data) -> {
              sagaBeginTime = System.currentTimeMillis();
              SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(context().system()).doSagaBeginCounter();
              SagaStartedDomain domainEvent = new SagaStartedDomain(event);
              if (event.getTimeout() > 0) {
                data.setTimeout(event.getTimeout());
                return goTo(SagaActorState.READY)
                    .applying(domainEvent)
                    .forMax(Duration.create(event.getTimeout(), TimeUnit.SECONDS));
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
              AddTxEventDomain domainEvent = new AddTxEventDomain(event);
              if (data.getExpirationTime() != null) {
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
              SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.SUSPENDED, SuspendedType.UNPREDICTABLE);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent);
            }
        ).event(SagaAbortedEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.SUSPENDED, SuspendedType.UNPREDICTABLE);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent);
            }
        ).event(Collections.singletonList(StateTimeout()), SagaData.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(null, SagaActorState.SUSPENDED, SuspendedType.TIMEOUT);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent);
            })
    );

    when(SagaActorState.PARTIALLY_ACTIVE,
        matchEvent(TxEndedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              if (data.getExpirationTime() != null) {
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
              AddTxEventDomain domainEvent = new AddTxEventDomain(event);
              if (data.getExpirationTime() != null) {
                return stay()
                    .applying(domainEvent)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return stay().applying(domainEvent);
              }
            }
        ).event(SagaTimeoutEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.SUSPENDED,
                  SuspendedType.TIMEOUT);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent);
            }
        ).event(TxAbortedEvent.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              return goTo(SagaActorState.FAILED)
                  .applying(domainEvent);
            }
        ).event(Collections.singletonList(StateTimeout()), SagaData.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(null, SagaActorState.SUSPENDED, SuspendedType.TIMEOUT);
              return goTo(SagaActorState.SUSPENDED).applying(domainEvent);
            })
    );

    when(SagaActorState.PARTIALLY_COMMITTED,
        matchEvent(TxStartedEvent.class,
            (event, data) -> {
              AddTxEventDomain domainEvent = new AddTxEventDomain(event);
              if (data.getExpirationTime() != null) {
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
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              if (data.getExpirationTime() != null) {
                return stay()
                    .applying(domainEvent)
                    .forMax(Duration.create(data.getTimeout(), TimeUnit.MILLISECONDS));
              } else {
                return stay().applying(domainEvent);
              }
            }
        ).event(SagaTimeoutEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.SUSPENDED, SuspendedType.TIMEOUT);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent);
            }
        ).event(SagaEndedEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.COMMITTED);
              return goTo(SagaActorState.COMMITTED)
                  .applying(domainEvent);
            }
        ).event(SagaAbortedEvent.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.FAILED);
              return goTo(SagaActorState.FAILED).applying(domainEvent);
            }
        ).event(TxAbortedEvent.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              return goTo(SagaActorState.FAILED).applying(domainEvent);
            }
        ).event(Collections.singletonList(StateTimeout()), SagaData.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(null, SagaActorState.SUSPENDED, SuspendedType.TIMEOUT);
              return goTo(SagaActorState.SUSPENDED).applying(domainEvent);
            })
    );

    when(SagaActorState.FAILED,
        matchEvent(SagaTimeoutEvent.class, SagaData.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.SUSPENDED, SuspendedType.TIMEOUT);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent);
            }
        ).event(TxCompensatedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              return stay().applying(domainEvent).andThen(exec(_data -> {
                self().tell(ComponsitedCheckEvent.builder().build(), self());
              }));
            }
        ).event(ComponsitedCheckEvent.class, SagaData.class,
            (event, data) -> {
              if (hasCompensationSentTx(data) || !data.isTerminated()) {
                return stay();
              } else {
                SagaEndedDomain domainEvent = new SagaEndedDomain(event,
                    SagaActorState.COMPENSATED);
                return goTo(SagaActorState.COMPENSATED)
                    .applying(domainEvent);
              }
            }
        ).event(SagaAbortedEvent.class, SagaData.class,
            (event, data) -> {
              data.setTerminated(true);
              if (hasCommittedTx(data)) {
                SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.FAILED);
                return stay()
                    .applying(domainEvent);
              } else if (hasCompensationSentTx(data)) {
                SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.FAILED);
                return stay()
                    .applying(domainEvent);
              } else {
                SagaEndedDomain domainEvent = new SagaEndedDomain(event,
                    SagaActorState.COMPENSATED);
                return goTo(SagaActorState.COMPENSATED)
                    .applying(domainEvent);
              }
            }
        ).event(TxStartedEvent.class, SagaData.class,
            (event, data) -> {
              AddTxEventDomain domainEvent = new AddTxEventDomain(event);
              return stay().applying(domainEvent);
            }
        ).event(TxEndedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              return stay().applying(domainEvent).andThen(exec(_data -> {
                TxEntity txEntity = _data.getTxEntityMap().get(event.getLocalTxId());
                // call compensate
                compensation(txEntity, _data);
              }));
            }
        ).event(Arrays.asList(StateTimeout()), SagaData.class,
            (event, data) -> {
              SagaEndedDomain domainEvent = new SagaEndedDomain(SagaActorState.SUSPENDED, SuspendedType.TIMEOUT);
              return goTo(SagaActorState.SUSPENDED)
                  .applying(domainEvent);
            })
    );

    when(SagaActorState.COMMITTED,
        matchEvent(org.apache.servicecomb.pack.alpha.core.fsm.event.internal.StopEvent.class,
            (event, data) -> {
              //  已经停止的Actor使用以下两个命令清理，但是 highestSequenceNr 不会被删除，需要手工清理
              //  以下基于 journal-redis 说明:
              //    假设 globalTxId=ed2cdb9c-e86c-4b01-9f43-8e34704e7694, 那么在 Redis 中会生成三个 key
              //    journal:persistenceIds
              //    journal:persisted:ed2cdb9c-e86c-4b01-9f43-8e34704e7694
              //    journal:persisted:ed2cdb9c-e86c-4b01-9f43-8e34704e7694:highestSequenceNr
              //
              //    1. journal:persistenceIds 是 set 类型, 记录了所有的 globalTxId, 使用 smembers journal:persistenceIds 可以看到
              //    2. journal:persisted:ed2cdb9c-e86c-4b01-9f43-8e34704e7694 是 zset 类型, 记录了这个事务的所有事件
              //       使用 zrange journal:persisted:ed2cdb9c-e86c-4b01-9f43-8e34704e7694 1 -1 可以看到
              //    3. journal:persisted:ed2cdb9c-e86c-4b01-9f43-8e34704e7694:highestSequenceNr 是 string 类型, 里面记录这序列号
              //
              //    何如清理:
              //      通过 deleteMessages 和 deleteSnapshot 可以清理部分数据，但是 highestSequenceNr 还是无法自动删除，需要定期手动清理
              //      遍历 journal:persistenceIds 集合，用每一条数据item拼接成key journal:persisted:item 和 journal:persisted:item:highestSequenceNr
              //      如果没有成对出现就说明是已经终止的actor 那么可以将 journal:persisted:item 从 journal:persistenceIds 删除
              //      并删除 journal:persisted:item:highestSequenceNr
              //
              //  目前可以看到的解释是 https://github.com/akka/akka/issues/21181
              deleteMessages(lastSequenceNr());
              deleteSnapshot(snapshotSequenceNr());
              return stop();
            }
        )
    );

    when(SagaActorState.SUSPENDED,
        matchEvent(org.apache.servicecomb.pack.alpha.core.fsm.event.internal.StopEvent.class,
            (event, data) -> {
              deleteMessages(lastSequenceNr());
              deleteSnapshot(snapshotSequenceNr());
              return stop();
            }
        )
    );

    when(SagaActorState.COMPENSATED,
        matchEvent(org.apache.servicecomb.pack.alpha.core.fsm.event.internal.StopEvent.class,
            (event, data) -> {
              deleteMessages(lastSequenceNr());
              deleteSnapshot(snapshotSequenceNr());
              return stop();
            }
        )
    );

    whenUnhandled(
        matchAnyEvent((event, data) -> {
          LOG.error("Unhandled event {}", event);
          return stay();
        })
    );

    onTransition(
        matchState(null, null, (from, to) -> {
          if (stateData().getGlobalTxId() != null) {
            stateData().setLastState(to);
            SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(getContext().getSystem())
                .putSagaData(stateData().getGlobalTxId(), stateData());
          }
          if (LOG.isDebugEnabled()) {
            LOG.debug("transition {} {} -> {}", getSelf(), from, to);
          }
          if (to == SagaActorState.COMMITTED ||
              to == SagaActorState.SUSPENDED ||
              to == SagaActorState.COMPENSATED) {
            self().tell(org.apache.servicecomb.pack.alpha.core.fsm.event.internal.StopEvent.builder().build(), self());
          }
        })
    );

    onTermination(
        matchStop(
            Normal(), (state, data) -> {
              if (LOG.isDebugEnabled()) {
                LOG.debug("stop {} {}", data.getGlobalTxId(), state);
              }
              sagaEndTime = System.currentTimeMillis();
              SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(context().system()).doSagaEndCounter();
              SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(context().system()).doSagaAvgTime(sagaEndTime - sagaBeginTime);
              data.setLastState(state);
              data.setEndTime(new Date());
              data.setTerminated(true);
              SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(getContext().getSystem())
                  .stopSagaData(data.getGlobalTxId(), data);
            }
        )
    );

  }

  @Override
  public SagaData applyEvent(DomainEvent event, SagaData data) {
    // log event to SagaData
    if (event.getEvent() != null && !(event
        .getEvent() instanceof ComponsitedCheckEvent)) {
      data.logEvent(event.getEvent());
    }
    if (event instanceof SagaStartedDomain) {
      SagaStartedDomain domainEvent = (SagaStartedDomain) event;
      data.setServiceName(domainEvent.getEvent().getServiceName());
      data.setInstanceId(domainEvent.getEvent().getInstanceId());
      data.setGlobalTxId(domainEvent.getEvent().getGlobalTxId());
      data.setBeginTime(domainEvent.getEvent().getCreateTime());
      data.setExpirationTime(domainEvent.getExpirationTime());
    } else if (event instanceof AddTxEventDomain) {
      AddTxEventDomain domainEvent = (AddTxEventDomain) event;
      if (!data.getTxEntityMap().containsKey(domainEvent.getEvent().getLocalTxId())) {
        TxEntity txEntity = TxEntity.builder()
            .serviceName(domainEvent.getEvent().getServiceName())
            .instanceId(domainEvent.getEvent().getInstanceId())
            .globalTxId(domainEvent.getEvent().getGlobalTxId())
            .localTxId(domainEvent.getEvent().getLocalTxId())
            .parentTxId(domainEvent.getEvent().getParentTxId())
            .compensationMethod(domainEvent.getCompensationMethod())
            .payloads(domainEvent.getPayloads())
            .state(domainEvent.getState())
            .build();
        data.getTxEntityMap().put(txEntity.getLocalTxId(), txEntity);
      } else {
        LOG.warn("TxEntity {} already exists", domainEvent.getEvent().getLocalTxId());
      }
    } else if (event instanceof UpdateTxEventDomain) {
      UpdateTxEventDomain domainEvent = (UpdateTxEventDomain) event;
      TxEntity txEntity = data.getTxEntityMap().get(domainEvent.getLocalTxId());
      txEntity.setEndTime(new Date());
      if (domainEvent.getState() == TxState.COMMITTED) {
        txEntity.setState(domainEvent.getState());
      } else if (domainEvent.getState() == TxState.FAILED) {
        txEntity.setState(domainEvent.getState());
        txEntity.setThrowablePayLoads(domainEvent.getThrowablePayLoads());
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
        data.setTerminated(true);
        data.getTxEntityMap().forEach((k, v) -> {
          if (v.getState() == TxState.COMMITTED) {
            // call compensate
            compensation(v, data);
          }
        });
      } else if (domainEvent.getState() == SagaActorState.SUSPENDED) {
        data.setEndTime(new Date());
        data.setTerminated(true);
        data.setSuspendedType(domainEvent.getSuspendedType());
      } else if (domainEvent.getState() == SagaActorState.COMPENSATED) {
        data.setEndTime(new Date());
        data.setTerminated(true);
      } else if (domainEvent.getState() == SagaActorState.COMMITTED) {
        data.setEndTime(new Date());
        data.setTerminated(true);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("applyEvent: {} {}", stateName(), stateData().getGlobalTxId());
    }
    return data;
  }

  @Override
  public void onRecoveryCompleted() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("onRecoveryCompleted: {} {}", stateName(), stateData().getGlobalTxId());
    }
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

  //call omega compensate method
  private void compensation(TxEntity txEntity, SagaData data) {
    // increments the compensation running counter by one
    data.getCompensationRunningCounter().incrementAndGet();
    txEntity.setState(TxState.COMPENSATION_SENT);
    try {
      SpringAkkaExtension.SPRING_EXTENSION_PROVIDER.get(context().system()).compensate(txEntity);
      LOG.info("compensate {}", txEntity.getLocalTxId());
    } catch (AlphaException ex) {
      LOG.error(ex.getMessage(), ex);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOG.error(e.getMessage(), e);
      }
      compensation(txEntity, data);
    } catch (Exception ex) {
      LOG.error("compensation failed " + txEntity.getLocalTxId(), ex);
      if (txEntity.getRetries() > 0) {
        // which means the retry number
        if (txEntity.getRetriesCounter().incrementAndGet() < txEntity.getRetries()) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
          }
          compensation(txEntity, data);
        }
      } else if (txEntity.getRetries() == -1) {
        // which means retry it until succeed
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          LOG.error(e.getMessage(), e);
        }
        compensation(txEntity, data);
      }
    }
  }
}
