package org.apache.servicecomb.pack.alpha.fsm;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;

public class SagaShardRegionActor extends AbstractActor {

  private final ActorRef workerRegion;

  static ShardRegion.MessageExtractor messageExtractor = new ShardRegion.MessageExtractor() {
    @Override
    public String entityId(Object message) {
      if (message instanceof BaseEvent) {
        return ((BaseEvent) message).getGlobalTxId();
      } else {
        return null;
      }
    }

    @Override
    public Object entityMessage(Object message) {
      return message;
    }

    @Override
    public String shardId(Object message) {
      int numberOfShards = 100;
      if (message instanceof BaseEvent) {
        String actorId = ((BaseEvent) message).getGlobalTxId();
        return String.valueOf(actorId.hashCode() % numberOfShards);
      } else if (message instanceof ShardRegion.StartEntity) {
        String actorId = ((ShardRegion.StartEntity) message).entityId();
        return String.valueOf(actorId.hashCode() % numberOfShards);
      } else {
        return null;
      }
    }
  };

  public SagaShardRegionActor() {
    ActorSystem system = getContext().getSystem();
    ClusterShardingSettings settings = ClusterShardingSettings.create(system);
    workerRegion = ClusterSharding.get(system)
        .start(
            "saga-actor",
            Props.create(SagaActor.class),
            settings,
            messageExtractor);
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .matchAny(msg -> {
          workerRegion.tell(msg, getSelf());
        })
        .build();
  }
}
