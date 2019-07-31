package org.apache.servicecomb.pack.alpha.fsm.channel.kafka.test;

import org.apache.servicecomb.pack.alpha.core.NodeStatus;
import org.apache.servicecomb.pack.alpha.core.fsm.sink.ActorEventSink;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KafkaApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaApplication.class, args);
    }

    @Bean(name = "actorEventSink")
    public ActorEventSink actorEventSink(){
        return new KafkaActorEventSink();
    }

    @Bean(name = "nodeStatus")
    public NodeStatus nodeStatus(){
        return new NodeStatus(NodeStatus.TypeEnum.MASTER);
    }
}
