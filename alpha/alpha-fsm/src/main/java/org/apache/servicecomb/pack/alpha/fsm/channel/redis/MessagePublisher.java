package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

public interface MessagePublisher {

    void publish(Object data);

}
