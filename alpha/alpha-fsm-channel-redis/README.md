# FSM Redis channel
## Enabled Saga State Machine Module

Using `alpha.feature.akka.enabled=true` launch Alpha and Omega Side 
Using `alpha.feature.akka.channel.type=redis` launch Alpha and Omega Side 

```properties
alpha.feature.akka.enabled=true
alpha.feature.akka.channel.type=redis
```

setting spring boot redis
```
spring.redis.host=your_redis_host
spring.redis.port=your_redis_port
spring.redis.password=your_redis_password
```
