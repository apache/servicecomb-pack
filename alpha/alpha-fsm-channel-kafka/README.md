# FSM kafka channel
## Enabled Saga State Machine Module

Using `alpha.feature.akka.enabled=true` launch Alpha and Omega Side 
Using `alpha.feature.akka.channel.type=kafka` launch Alpha and Omega Side 

```properties
alpha.feature.akka.enabled=true
alpha.feature.akka.channel.type=kafka
```

setting spring boot kafka
```
spring.kafka.bootstrap-servers=kafka bootstrap_servers 
spring.kafka.consumer.group-id=kafka consumer group id, default servicecomb-pack
alpha.feature.akka.channel.kafka.topic= kafka topic name, default servicecomb-pack-actor-event
spring.kafka.producer.batch-size= producer batch size, default 16384
spring.kafka.producer.retries = producer retries, default 0
spring.kafka.producer.buffer.memory = producer buffer memory, default 33364432
spring.kafka.consumer.auto.offset.reset = consumer auto offset reset, default earliest
spring.kafka.consumer.enable.auto.commit = consumer enable auto commit, default false
spring.kafka.consumer.auto.commit.interval.ms = consumer auto commit interval ms, default 100
spring.kafka.listener.ackMode = consumer listener ack mode , default AckMode.MANUAL_IMMEDIATE
spring.kafka.listener.pollTimeout = consumer listener pool timeout, default 1500 ms
```
