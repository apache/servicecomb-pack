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
spring.kafka.consumer.group-id=kafka consumer group id
alpha.feature.akka.channel.kafka.topic= kafka topic name
```