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

package org.apache.servicecomb.pack.alpha.spec.saga.akka;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.sharding.ShardRegion;
import akka.persistence.fsm.AbstractPersistentFSM;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.servicecomb.pack.alpha.core.AlphaException;
import org.apache.servicecomb.pack.alpha.core.fsm.SuspendedType;
import org.apache.servicecomb.pack.alpha.core.fsm.TxState;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.SagaTimeoutEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxAbortedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckFailedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxCompensateAckSucceedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxEndedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.TxStartedEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.internal.CompensateAckTimeoutEvent;
import org.apache.servicecomb.pack.alpha.core.fsm.event.internal.ComponsitedCheckEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.domain.AddTxEventDomain;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.domain.DomainEvent;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.domain.SagaEndedDomain;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.domain.SagaStartedDomain;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.domain.UpdateTxEventDomain;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.SagaData;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.model.TxEntity;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka.SagaDataExtension;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.spring.integration.akka.SpringAkkaExtension;
import org.apache.servicecomb.pack.common.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

public class SagaActor extends
    AbstractPersistentFSM<SagaActorState, SagaData, DomainEvent> {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private String persistenceId;
  private long sagaBeginTime;
  private long sagaEndTime;

  public static Props props(String persistenceId) {
    return Props.create(SagaActor.class, persistenceId);
  }

  public SagaActor(String persistenceId) {
    if (persistenceId != null) {
      this.persistenceId = persistenceId;
    } else {
      this.persistenceId = getSelf().path().name();
    }

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
        ).event(TxCompensateAckSucceedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              return stay().applying(domainEvent).andThen(exec(_data -> {
                self().tell(ComponsitedCheckEvent.builder()
                    .serviceName(event.getServiceName())
                    .instanceId(event.getInstanceId())
                    .globalTxId(event.getGlobalTxId())
                    .localTxId(event.getLocalTxId())
                    .parentTxId(event.getParentTxId())
                    .preState(TxState.COMPENSATED_SUCCEED)
                    .build(), self());
              }));
            }
        ).event(TxCompensateAckFailedEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              return stay().applying(domainEvent).andThen(exec(_data -> {
                self().tell(ComponsitedCheckEvent.builder()
                    .serviceName(event.getServiceName())
                    .instanceId(event.getInstanceId())
                    .globalTxId(event.getGlobalTxId())
                    .localTxId(event.getLocalTxId())
                    .parentTxId(event.getParentTxId())
                    .preState(TxState.COMPENSATED_FAILED)
                    .build(), self());
              }));
            }
        ).event(CompensateAckTimeoutEvent.class, SagaData.class,
            (event, data) -> {
              UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
              return stay().applying(domainEvent).andThen(exec(_data -> {
                self().tell(ComponsitedCheckEvent.builder()
                    .serviceName(event.getServiceName())
                    .instanceId(event.getInstanceId())
                    .globalTxId(event.getGlobalTxId())
                    .localTxId(event.getLocalTxId())
                    .parentTxId(event.getParentTxId())
                    .preState(TxState.COMPENSATED_FAILED)
                    .build(), self());
              }));
            }
        ).event(ComponsitedCheckEvent.class, SagaData.class,
            (event, data) -> {
              if (data.getTxEntities().hasCompensationSentTx() ||
                  data.getTxEntities().hasCompensationFailedTx()) {
                UpdateTxEventDomain domainEvent = new UpdateTxEventDomain(event);
                return stay().applying(domainEvent);
              } else {
                if(data.getSuspendedType() == SuspendedType.COMPENSATE_FAILED) {
                  SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.SUSPENDED, SuspendedType.COMPENSATE_FAILED);
                  return goTo(SagaActorState.SUSPENDED).applying(domainEvent);
                } else {
                  SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.COMPENSATED);
                  return goTo(SagaActorState.COMPENSATED).applying(domainEvent);
                }
              }
            }
        ).event(SagaAbortedEvent.class, SagaData.class,
            (event, data) -> {
              if (data.getTxEntities().hasCommittedTx()) {
                SagaEndedDomain domainEvent = new SagaEndedDomain(event, SagaActorState.FAILED);
                return stay()
                    .applying(domainEvent);
              } else if (data.getTxEntities().hasCompensationSentTx()) {
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
                TxEntity txEntity = _data.getTxEntities().get(event.getLocalTxId());
                // call compensate
                compensation(domainEvent, txEntity, _data);
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
              beforeStop(event, stateName(), data);
              return stop();
            }
        )
    );

    when(SagaActorState.SUSPENDED,
        matchEvent(org.apache.servicecomb.pack.alpha.core.fsm.event.internal.StopEvent.class,
            (event, data) -> {
              beforeStop(event, stateName(), data);
              return stop();
            }
        )
    );

    when(SagaActorState.COMPENSATED,
        matchEvent(org.apache.servicecomb.pack.alpha.core.fsm.event.internal.StopEvent.class,
            (event, data) -> {
              beforeStop(event, stateName(), data);
              return stop();
            }
        )
    );

    whenUnhandled(
        matchAnyEvent((event, data) -> {
          if (event instanceof BaseEvent){
            LOG.debug("Unhandled event {}", event);
          }
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
            LOG.debug("transition [{}] {} -> {}", stateData().getGlobalTxId(), from, to);
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
              LOG.info("stopped [{}] {}", data.getGlobalTxId(), state);
            }
        )
    );

  }

  private void beforeStop(BaseEvent event, SagaActorState state, SagaData data){
    if (LOG.isDebugEnabled()) {
      LOG.debug("stop [{}] {}", data.getGlobalTxId(), state);
    }
    try{
      sagaEndTime = System.currentTimeMillis();
      data.setLastState(state);
      data.setEndTime(new Date());
      data.setTerminated(true);
      SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(getContext().getSystem())
          .stopSagaData(data.getGlobalTxId(), data);
      SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(context().system()).doSagaEndCounter();
      SagaDataExtension.SAGA_DATA_EXTENSION_PROVIDER.get(context().system())
          .doSagaAvgTime(sagaEndTime - sagaBeginTime);

      // destroy self from cluster shard region
      getContext().getParent()
          .tell(new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());

      //  clear self mailbox from persistence
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
      //
      //  Lua script akka-persistence-redis-clean.lua

      //  local ids = redis.call('smembers','journal:persistenceIds');
      //  local delkeys = {};
      //  for k, v in pairs(ids) do
      //    local jpid = 'journal:persisted:' .. v;
      //    local jpidnr = 'journal:persisted:' .. v .. ':highestSequenceNr';
      //    local hasjpid  = redis.call('exists',jpid);
      //    if(hasjpid == 0)
      //    then
      //      local hasjpidnr  = redis.call('exists',jpidnr);
      //      if(hasjpidnr == 1)
      //      then
      //        redis.call('del',jpidnr);
      //        table.insert(delkeys,jpid);
      //      end
      //    end
      //  end
      //  return delkeys;
      deleteMessages(lastSequenceNr());
      deleteSnapshot(snapshotSequenceNr());
    }catch(Exception e){
      LOG.error("stop [{}] fail",data.getGlobalTxId());
      throw e;
    }
  }

  @Override
  public SagaData applyEvent(DomainEvent event, SagaData data) {
    LOG.debug("apply domain event {}", event.getEvent());
    try{
      if (this.recoveryRunning()) {
        LOG.info("recovery {}",event.getEvent());
      }else if (LOG.isDebugEnabled()) {
        LOG.debug("persistence {}", event.getEvent());
      }
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
        if (!data.getTxEntities().exists(domainEvent.getEvent().getLocalTxId())) {
          TxEntity txEntity = TxEntity.builder()
              .serviceName(domainEvent.getEvent().getServiceName())
              .instanceId(domainEvent.getEvent().getInstanceId())
              .globalTxId(domainEvent.getEvent().getGlobalTxId())
              .localTxId(domainEvent.getEvent().getLocalTxId())
              .parentTxId(domainEvent.getEvent().getParentTxId())
              .compensationMethod(domainEvent.getCompensationMethod())
              .payloads(domainEvent.getPayloads())
              .state(domainEvent.getState())
              .reverseRetries(domainEvent.getReverseRetries())
              .reverseTimeout(domainEvent.getReverseTimeout())
              .retryDelayInMilliseconds(domainEvent.getRetryDelayInMilliseconds())
              .beginTime(domainEvent.getEvent().getCreateTime())
              .build();
          data.getTxEntities().put(txEntity.getLocalTxId(), txEntity);
        } else {
          LOG.warn("TxEntity {} already exists", domainEvent.getEvent().getLocalTxId());
        }
      } else if (event instanceof UpdateTxEventDomain) {
        UpdateTxEventDomain domainEvent = (UpdateTxEventDomain) event;
        TxEntity txEntity = data.getTxEntities().get(domainEvent.getLocalTxId());
        txEntity.setEndTime(domainEvent.getEvent().getCreateTime());
        if (domainEvent.getState() == TxState.COMMITTED) {
          txEntity.setState(domainEvent.getState());
        } else if (domainEvent.getState() == TxState.FAILED) {
          txEntity.setState(domainEvent.getState());
          txEntity.setThrowablePayLoads(domainEvent.getThrowablePayLoads());
          data.getTxEntities().forEachReverse((k, v) -> {
            if (v.getState() == TxState.COMMITTED) {
              // call compensate
              if (!compensation(domainEvent, v, data)) {
                return;
              }
            }
          });
        } else if (domainEvent.getState() == TxState.COMPENSATED_SUCCEED) {
          data.getCompensationRunningCounter().decrementAndGet();
          txEntity.setState(TxState.COMPENSATED_SUCCEED);
          LOG.info("compensate is succeed [{}] {}", txEntity.getGlobalTxId(), txEntity.getLocalTxId());
        } else if (domainEvent.getState() == TxState.COMPENSATED_FAILED) {
          data.getCompensationRunningCounter().decrementAndGet();
          txEntity.setState(TxState.COMPENSATED_FAILED);
          txEntity.setThrowablePayLoads(domainEvent.getThrowablePayLoads());
          if (txEntity.getReverseRetries() > 0 &&
              txEntity.getRetriesCounter().get() < txEntity.getReverseRetries()) {
            data.getTxEntities().forEachReverse((k, v) -> {
              if (v.getState() == TxState.COMMITTED || v.getState() == TxState.COMPENSATED_FAILED) {
                // call compensate
                if (!compensation(domainEvent, v, data)){
                  return;
                }
              }
            });
          } else {
            data.setSuspendedType(SuspendedType.COMPENSATE_FAILED);
            self().tell(ComponsitedCheckEvent.builder()
                .serviceName(txEntity.getServiceName())
                .instanceId(txEntity.getInstanceId())
                .globalTxId(txEntity.getGlobalTxId())
                .localTxId(txEntity.getLocalTxId())
                .preState(TxState.COMPENSATED_FAILED)
                .parentTxId(txEntity.getParentTxId()).build(), self());
          }
        }
      } else if (event instanceof SagaEndedDomain) {
        SagaEndedDomain domainEvent = (SagaEndedDomain) event;
        if (domainEvent.getState() == SagaActorState.FAILED) {
          data.getTxEntities().forEachReverse((k, v) -> {
            if (v.getState() == TxState.COMMITTED) {
              // call compensate
              if (!compensation(domainEvent, v, data)){
                return;
              }
            }
          });
        } else if (domainEvent.getState() == SagaActorState.SUSPENDED) {
          data.setEndTime(event.getEvent() != null ? event.getEvent().getCreateTime() : new Date());
          data.setSuspendedType(domainEvent.getSuspendedType());
        } else if (domainEvent.getState() == SagaActorState.COMPENSATED) {
          data.setEndTime(event.getEvent() != null ? event.getEvent().getCreateTime() : new Date());
        } else if (domainEvent.getState() == SagaActorState.COMMITTED) {
          data.setEndTime(event.getEvent() != null ? event.getEvent().getCreateTime() : new Date());
        }
      }
    }catch (Exception ex){
      LOG.error("apply {}", event.getEvent(), ex);
      LOG.error(ex.getMessage(), ex);
      beforeStop(event.getEvent(), SagaActorState.SUSPENDED, data);
      stop();
      //TODO 增加 SagaActor 处理失败指标
    }
    return data;
  }

  @Override
  public void onRecoveryCompleted() {
    if(stateName() != SagaActorState.IDLE){
      LOG.info("recovery completed [{}] state={}", stateData().getGlobalTxId(), stateName());
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

  //call omega compensate method
  private boolean compensation(DomainEvent event, TxEntity txEntity, SagaData data) {
    // increments the compensation running counter by one
    data.getCompensationRunningCounter().incrementAndGet();
    txEntity.setState(TxState.COMPENSATION_SENT);
    try {
      LOG.info("compensate {} {} [{}] {}", txEntity.getServiceName(), txEntity.getInstanceId(), txEntity.getGlobalTxId(), txEntity.getLocalTxId());
      SpringAkkaExtension.SPRING_EXTENSION_PROVIDER.get(context().system()).compensate(txEntity);
    } catch (Exception ex) {
      LOG.error("compensate failed [{}] {}", txEntity.getGlobalTxId(), txEntity.getLocalTxId(), ex);
      if (txEntity.getReverseRetries() > 0 &&
          txEntity.getRetriesCounter().incrementAndGet() < txEntity.getReverseRetries()) {
        LOG.info("Retry compensate {}/{} [{}] {} after {} ms",
            txEntity.getRetriesCounter().get() + 1,
            txEntity.getReverseRetries(),
            txEntity.getGlobalTxId(),
            txEntity.getLocalTxId(),
            txEntity.getRetryDelayInMilliseconds());
        try {
          Thread.sleep(txEntity.getRetryDelayInMilliseconds());
        } catch (InterruptedException e) {
          LOG.error(e.getMessage(), e);
        }
      }
      if (ex instanceof TimeoutException) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        String stackTrace = writer.toString();
        if (stackTrace.length() > Environment.getInstance().getPayloadsMaxLength()) {
          stackTrace = stackTrace.substring(0, Environment.getInstance().getPayloadsMaxLength());
        }
        CompensateAckTimeoutEvent compensateAckTimeoutEvent = CompensateAckTimeoutEvent.builder()
            .createTime(new Date(System.currentTimeMillis()))
            .globalTxId(txEntity.getGlobalTxId())
            .parentTxId(txEntity.getParentTxId())
            .localTxId(txEntity.getLocalTxId())
            .serviceName(txEntity.getServiceName())
            .instanceId(txEntity.getInstanceId())
            .payloads(stackTrace.getBytes())
            .build();
        self().tell(compensateAckTimeoutEvent, self());
      }
      if (ex instanceof AlphaException) {
        self().tell(TxCompensateAckFailedEvent.builder()
            .serviceName(txEntity.getServiceName())
            .instanceId(txEntity.getInstanceId())
            .globalTxId(txEntity.getGlobalTxId())
            .localTxId(txEntity.getLocalTxId())
            .parentTxId(txEntity.getParentTxId())
            .payloads(ex.getMessage().getBytes())
            .build(), self());
      }
      return false;
    }
    return true;
  }
}