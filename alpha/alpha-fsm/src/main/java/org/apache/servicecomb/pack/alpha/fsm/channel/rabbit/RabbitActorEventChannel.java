package org.apache.servicecomb.pack.alpha.fsm.channel.rabbit;

import org.apache.servicecomb.pack.alpha.core.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.channel.AbstractActorEventChannel;
import org.apache.servicecomb.pack.alpha.fsm.metrics.MetricsService;

public class RabbitActorEventChannel extends AbstractActorEventChannel {

    private RabbitMessagePublisher rabbitMqMessagePublisher;

    public RabbitActorEventChannel(MetricsService metricsService, RabbitMessagePublisher rabbitMqMessagePublisher) {
        super(metricsService);
        this.rabbitMqMessagePublisher = rabbitMqMessagePublisher;
    }

    @Override
    public void sendTo(BaseEvent event) {
        rabbitMqMessagePublisher.publish(event);
    }
}
