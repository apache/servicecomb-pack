package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.sink.ActorEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

public class RedisMessageSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private ActorEventSink actorEventSink;

    public RedisMessageSubscriber(ActorEventSink actorEventSink) {
        this.actorEventSink = actorEventSink;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        logger.info("message = [{}] and pattern = [{}]", message.toString(), new String(pattern, StandardCharsets.UTF_8));
        try {
            BaseEvent baseEvent = (BaseEvent) message;
            actorEventSink.send(baseEvent);
        }catch (Exception e){
            logger.error("subscriber Exception = [{}]", e);
        }
    }
}
