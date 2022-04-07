# 事件通道

Alpha 收到 Omeag 发送的事件后放入事件通道等待 Akka 处理，事件通道有三种实现方式，一种是内存通道另外是 Kafka,Rabbit 通道

| 通道类型 | 模式 | 说明                                                         |
| -------- | ---- | ------------------------------------------------------------ |
| memory   | 单例 | 使用内存作为数据通道，不建议在生产环境使用                   |
| kafka    | 集群 | 使用 Kafka 作为数据通道，使用全局事务ID作为分区策略，集群中的所有节点同时工作，可水平扩展，当配置了 spring.profiles.active=prd,cluster 参数后默认就使用 kafka 通道 |
| rabbit    | 集群 | 使用 rabbit 作为数据通道，使用全局事务ID作为分区策略, 由于rabbit 原生不支持分区，所以引用了  [spring-cloud-stream](https://github.com/spring-cloud/spring-cloud-stream-binder-rabbit)  |

 可以使用参数 `alpha.spec.saga.akka.channel.name` 配置通道类型

- Memory 通道参数

| 参数名                                 | 参数值 | 说明                                        |
| -------------------------------------- | ------ | ------------------------------------------- |
| alpha.spec.saga.akka.channel.name       | memory |                                             |
| alpha.spec.saga.akka.channel.max-length | -1     | momory类型时内存队列大小，-1表示Integer.MAX |

- Kafka 通道参数

| 参数名                                                           | 参数值   | 说明                                        |
|---------------------------------------------------------------| -------- | ------------------------------------------- |
| alpha.spec.saga.akka.channel.name                             | kafka    |                                             |
| alpha.spec.saga.akka.channel.kafka.bootstrap-servers          | -1       | momory类型时内存队列大小，-1表示Integer.MAX |
| alpha.spec.saga.akka.channel.kafka.producer.batch-size        | 16384    |                                             |
| alpha.spec.saga.akka.channel.kafka.producer.retries           | 0        |                                             |
| alpha.spec.saga.akka.channel.kafka.producer.buffer.memory     | 33554432 |                                             |
| alpha.spec.saga.akka.channel.kafka.consumer.auto.offset.reset | earliest |                                             |
| alpha.spec.saga.akka.channel.kafka.listener.pollTimeout       | 1500     |                                             |
| alpha.spec.saga.akka.channel.kafka.numPartitions              | 6        |                                             |
| alpha.spec.saga.akka.channel.kafka.replicationFactor          | 1        |                                             |

- Rabbit 通道参数

| 参数名                                                                                       | 参数值                        | 说明                           |
|-------------------------------------------------------------------------------------------|----------------------------|------------------------------|
| alpha.spec.saga.akka.channel.name                                                         | rabbit                     |                              |
| spring.cloud.stream.instance-index                                                        | 0                          | 分区索引                         | 
| spring.cloud.stream.instance-count                                                        | 1                          |                              |
| spring.cloud.stream.bindings.service-comb-pack-producer.producer.partition-count          | 1                          | 分区数量，分区数量需要与alpha-server保持一致 |
| spring.cloud.stream.binders.defaultRabbit.environment.spring.rabbitmq.virtual-host        | servicecomb-pack           |                              |
| spring.cloud.stream.binders.defaultRabbit.environment.spring.rabbitmq.host                | rabbitmq.servicecomb.io    |                              |
| spring.cloud.stream.binders.defaultRabbit.environment.spring.rabbitmq.username            | servicecomb-pack           |                              |
| spring.cloud.stream.binders.defaultRabbit.environment.spring.rabbitmq.password            | H123213PWD                 |                              |
| spring.cloud.stream.binders.defaultRabbit.type                                            | rabbit                     |                              |
| spring.cloud.stream.bindings.service-comb-pack-producer.destination                       | exchange-service-comb-pack |                              |
| spring.cloud.stream.bindings.service-comb-pack-producer.content-type                      | application/json           |                              |
| spring.cloud.stream.bindings.service-comb-pack-producer.producer.partition-key-expression | headers['partitionKey']    | 分区表达式                        |  
| spring.cloud.stream.bindings.service-comb-pack-consumer.group                             | group-pack                 |                              |
| spring.cloud.stream.bindings.service-comb-pack-consumer.content-type                      | application/json           |                              |
| spring.cloud.stream.bindings.service-comb-pack-consumer.destination                       | exchange-service-comb-pack |                              |
| spring.cloud.stream.bindings.service-comb-pack-consumer.consumer.partitioned              | true                       |                              |

                                          

