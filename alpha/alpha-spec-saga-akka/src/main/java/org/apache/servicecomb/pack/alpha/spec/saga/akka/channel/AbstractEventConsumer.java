package org.apache.servicecomb.pack.alpha.spec.saga.akka.channel;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.apache.servicecomb.pack.alpha.spec.saga.akka.metrics.MetricsService;

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
