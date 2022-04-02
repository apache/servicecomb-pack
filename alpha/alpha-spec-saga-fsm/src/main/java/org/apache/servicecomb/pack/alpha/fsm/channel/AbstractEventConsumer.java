package org.apache.servicecomb.pack.alpha.fsm.channel;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;

public abstract class AbstractEventConsumer {

  protected final MetricsService metricsService;
  protected final ActorSystem actorSystem;
  protected final ActorRef sagaShardRegionActor;

  public AbstractEventConsumer(
      ActorSystem actorSystem,
      ActorRef sagaShardRegionActor, MetricsService metricsService) {
    this.metricsService = metricsService;
    this.actorSystem = actorSystem;
    this.sagaShardRegionActor = sagaShardRegionActor;
  }
}
