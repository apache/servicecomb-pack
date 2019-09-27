# 事件通道

Alpha 收到 Omeag 发送的事件后放入事件通道等待 Akka 处理，事件通道有两种实现方式，一种是内存通道另一种是 Kafka 通道

| 通道类型 | 模式 | 说明                                                         |
| -------- | ---- | ------------------------------------------------------------ |
| memory   | 单例 | 使用内存作为数据通道，不建议在生产环境使用                   |
| kafka    | 集群 | 使用 Kafka 作为数据通道，使用全局事务ID作为分区策略，集群中的所有节点同时工作，可水平扩展，当配置了 spring.profiles.active=prd,cluster 参数后默认就使用 kafka 通道 |

 可以使用参数 `alpha.feature.akka.channel.type` 配置通道类型

- Memory 通道参数

| 参数名                                 | 参数值 | 说明                                        |
| -------------------------------------- | ------ | ------------------------------------------- |
| alpha.feature.akka.channel.type        | memory |                                             |
| alpha.feature.akka.channel.memory.size | -1     | momory类型时内存队列大小，-1表示Integer.MAX |

- Kafka 通道参数

| 参数名                                  | 参数值   | 说明                                        |
| --------------------------------------- | -------- | ------------------------------------------- |
| alpha.feature.akka.channel.type         | kafka    |                                             |
| spring.kafka.bootstrap-servers          | -1       | momory类型时内存队列大小，-1表示Integer.MAX |
| spring.kafka.producer.batch-size        | 16384    |                                             |
| spring.kafka.producer.retries           | 0        |                                             |
| spring.kafka.producer.buffer.memory     | 33554432 |                                             |
| spring.kafka.consumer.auto.offset.reset | earliest |                                             |
| spring.kafka.listener.pollTimeout       | 1500     |                                             |
| kafka.numPartitions                     | 6        |                                             |
| kafka.replicationFactor                 | 1        |                                             |



