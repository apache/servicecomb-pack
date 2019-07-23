package org.apache.servicecomb.pack.alpha.fsm.channel.redis;

public interface MessagePublisher {

    void publish(String message);

}
