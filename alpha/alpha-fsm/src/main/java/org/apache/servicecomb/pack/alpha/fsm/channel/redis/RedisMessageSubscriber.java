package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

import org.apache.servicecomb.pack.alpha.fsm.event.base.BaseEvent;
import org.apache.servicecomb.pack.alpha.fsm.sink.ActorEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

public class RedisMessageSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    private ActorEventSink actorEventSink;

    public RedisMessageSubscriber(ActorEventSink actorEventSink) {
        this.actorEventSink = actorEventSink;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        logger.info("pattern = [{}]",  new String(pattern, StandardCharsets.UTF_8));
        try {
            BaseEvent event = (BaseEvent) deserialize(message.getBody());
            logger.info("event = [{}]", event);

            if(null != event) {
                actorEventSink.send(event);
            }else{
                logger.warn("onMessage baseEvent is null");
            }
        }catch (Exception e){
            logger.error("subscriber Exception = [{}]", e);
        }
    }
    public Object deserialize(byte[] bytes) {
        try {
            ByteArrayInputStream boas = new ByteArrayInputStream(bytes);
            ObjectInputStream ous = new ObjectInputStream(boas);
            Object object = ous.readObject();
            ous.close();
            return object;
        } catch (IOException | ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        return null;

    }

}
