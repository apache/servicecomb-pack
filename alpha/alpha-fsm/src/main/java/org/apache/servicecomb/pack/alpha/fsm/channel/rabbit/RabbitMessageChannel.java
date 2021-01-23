package org.apache.servicecomb.pack.alpha.fsm.channel.rabbit;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface RabbitMessageChannel {


    String SERVICE_COMB_PACK_PRODUCER = "service-comb-pack-producer";
    String SERVICE_COMB_PACK_CONSUMER = "service-comb-pack-consumer";


    @Output(SERVICE_COMB_PACK_PRODUCER)
    MessageChannel messageChannel();

    @Input(SERVICE_COMB_PACK_CONSUMER)
    SubscribableChannel input();
}
