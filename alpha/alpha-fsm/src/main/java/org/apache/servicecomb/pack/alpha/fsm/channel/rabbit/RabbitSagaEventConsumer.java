

package org.apache.servicecomb.pack.alpha.fsm.channel.rabbit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.channel.AbstractEventConsumer;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class RabbitSagaEventConsumer extends AbstractEventConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public RabbitSagaEventConsumer(ActorSystem actorSystem, ActorRef sagaShardRegionActor,
                                   MetricsService metricsService) {
        super(actorSystem, sagaShardRegionActor, metricsService);

    }

    @StreamListener(org.apache.servicecomb.pack.alpha.fsm.channel.rabbit.RabbitMessageChannel.SERVICE_COMB_PACK_CONSUMER)
    public void receive(BaseEvent baseEvent) {
        sendSagaActor(baseEvent);
    }


    private CompletionStage<String> sendSagaActor(BaseEvent event) {
        try {
            long begin = System.currentTimeMillis();
            metricsService.metrics().doActorReceived();
            Timeout timeout = new Timeout(Duration.create(10, "seconds"));
            Future<Object> future = Patterns.ask(sagaShardRegionActor, event, timeout);
            Await.result(future, timeout.duration());
            long end = System.currentTimeMillis();
            metricsService.metrics().doActorAccepted();
            metricsService.metrics().doActorAvgTime(end - begin);
            return CompletableFuture.completedFuture("OK");
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            metricsService.metrics().doActorRejected();
            throw new CompletionException(ex);
        }
    }
}
