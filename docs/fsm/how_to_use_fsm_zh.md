# Alpha 状态机模式

## 快速开始

状态机模式使用 Elasticsearch 存储已结束的事务数据

* 启动 Elasticsearch

  ```bash
  docker run --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:6.6.2
  ```

* 启动 Alpha
  使用 `alpha.feature.akka.enabled=true` 开启状态机模式

  ```bash
  java -jar alpha-server-${version}-exec.jar \
    --spring.datasource.url=jdbc:postgresql://0.0.0.0:5432/saga?useSSL=false \
    --spring.datasource.username=saga-user \
    --spring.datasource.password=saga-password \
    --spring.profiles.active=prd \
    --alpha.feature.akka.enabled=true \
    --alpha.feature.akka.transaction.repository.type=elasticsearch \
    --spring.data.elasticsearch.cluster-name=docker-cluster \
    --spring.data.elasticsearch.cluster-nodes=localhost:9300  
  ```

  更多持久化参数参见 "事务数据持久化" 说明

  **注意：** 参数 `spring.data.elasticsearch.cluster-name` 设置的是 Elasticsearch 集群名称，使用 docker 启动 Elasticsearch 默认集群名称是 `docker-cluster` , 你可以使用 `curl http://localhost:9200/` 命令查询

* Omega 侧配置

  使用 `alpha.feature.akka.enabled=true` 开启状态机模式

  ```base
  alpha.feature.akka.enabled=true
  ```
  
* WEB管理界面

  在浏览器中打开 http://localhost:8090/admin

  仪表盘

  ![image-20190809122237766](assets/ui-dashboard.png)

  事务列表

  ![image-20190809122324563](assets/ui-transactions-list.png)

  事务明细-成功

  ![image-20190809122352852](assets/ui-transaction-details-successful.png)

  事务明细-补偿

  ![image-20190809122516345](assets/ui-transaction-details-compensated.png)
  
  事务明细-失败

  ![image-20190809122442186](assets/ui-transaction-details-failed.png)

## APIs

#### 性能度量

你可以使用 API 查询 Alpha 的性能指标，你可以使用基准测试工具 `AlphaBenchmark` 模拟发送数据后快速体验这一功能

例如：使用以下命令模拟 10 并发，发送 1000 个全局事务

```bash
java -jar alpha-benchmark-0.5.0-SNAPSHOT-exec.jar --alpha.cluster.address=0.0.0.0:8080 --w=0 --n=1000 --c=10
```

查询性能指标

```bash
curl http://localhost:8090/alpha/api/v1/metrics

{
  nodeType: "MASTER",
  metrics: {
    eventReceived: 8000,
    eventAccepted: 8000,
    eventRejected: 0,
    eventAvgTime: 0,
    actorReceived: 8000,
    actorAccepted: 8000,
    actorRejected: 0,
    actorAvgTime: 0,
    sagaBeginCounter: 1000,
    sagaEndCounter: 1000,
    sagaAvgTime: 9,
    committed: 1000,
    compensated: 0,
    suspended: 0,
    repositoryReceived: 1000,
    repositoryAccepted: 1000,
    repositoryRejected: 0,
    repositoryAvgTime: 0.88
  }
}
```

例如以上指标中显示 `sagaAvgTime: 9` 表示每个全局事务在Akka的处理耗时9毫秒，`repositoryAvgTime: 0.88` 表示每个全局事务入库耗时0.88毫秒

指标说明

* eventReceived: Alpha 收到的 gRPC 事件数量
* eventAccepted:  Alpha 处理的 gRPC 事件数量（事件放入事件通道）
* eventRejected:  Alpha 拒绝的 gRPC 事件数量
* eventAvgTime: Alpha 平均耗时（毫秒）
* actorReceived: Akka 收到的事件数量
* actorAccepted:  Akka 处理的事件数量
* actorRejected: Akka 拒绝的事件数量
* actorAvgTime: Akka 平均耗时（毫秒）
* sagaBeginCounter: 开始的 Saga 全局事务数量
* sagaEndCounter: 结束的 Saga 全局事务数量
* sagaAvgTime: 平均耗时（毫秒）
* committed: COMMITTED状态的 Saga 全局事务数量
* compensated: COMPENSATED状态的 Saga 全局事务数量
* suspended: SUSPENDED状态的 Saga 的全局事务数量
* repositoryReceived: 存储模块收到的全局事务数量
* repositoryAccepted: 存储模块处理的全局事务数量
* repositoryRejected: 存储模块拒绝的全局事务数量
* repositoryAvgTime: 平均耗时（毫秒）

#### 事务数据查询

> 需要启动 Elasticsearch 存储事务

* 查询事务列表

  ```bash
  curl -X GET http://localhost:8090/alpha/api/v1/transaction?page=0&size=50
  
  {
    "total": 2002,
    "page": 0,
    "size": 50,
    "elapsed": 581,
    "globalTransactions": [...]
  }
  ```

  请求参数

  * page 页号

  * size 返回行数

  返回参数

  * total 总行数
  * page 本次查询结果页号
  * size 本次查询行数
  * elapsed 本次查询耗时（毫秒）
  * globalTransactions 事件数据列表

* 查询一条事务

  ```bash
  curl -X GET http://localhost:8090/alpha/api/v1/transaction/{globalTxId}
  
  {
    "globalTxId": "e00a3bac-de6b-498f-99a4-c11d3087fd14",
    "type": "SAGA",
    "serviceName": "alpha-benchmark",
    "instanceId": "alpha-benchmark-127.0.0.1",
    "beginTime": 1564762932963,
    "endTime": 1564762933197,
    "state": "COMMITTED",
    "subTxSize": 3,
    "durationTime": 408,
    "subTransactions": [...],
    "events": [...]
  }
  ```

  请求参数

  * globalTxId 全局事务ID

  返回参数

  * globalTxId 全局事务ID
  * type 事务类型，目前只有SAGA，后期增加TCC
  * serviceName 全局事务发起方服务名称
  * instanceId 全局事务发起方实例ID
  * beginTime 事务开始时间
  * endTime 事务结束时间
  * state 事务最终状态
  * subTxSize 包含子事务数量
  * durationTime 全局事务处理耗时
  * subTransactions 子事务数据列表
  * events 事件列表

## 事务数据持久化

只有结束的事务才会被持久化到 Elasticsearch，执行中的事务数据通过Akka持久化。事务结束状态有以下几种

* 事务成功结束，最后状态为 COMMITTED

* 事务补偿后结束，最后状态为 COMPENSATED

* 事务异常结束，最后状态为 SUSPENDED

  导致事务异常结束有以下几种情况

  1. 事务超时
  2. Alpha收到了不符合预期的事件，例如在 收到 TxStartedEvent 前就收到了 TxEndedEvent，或者没有收到任何子事务事件就收到了 SagaEndedEvent等，这些规则都被定义在了有限状态机中。

### 持久化参数

| 参数名                                                       | 默认值 | 说明                                                         |
| ------------------------------------------------------------ | ------ | ------------------------------------------------------------ |
| alpha.feature.akka.transaction.repository.type               |        | 持久化类型，目前可选值 elasticsearch，如果不设置则不存储     |
| alpha.feature.akka.transaction.repository.elasticsearch.memory.size | -1     | 持久化数据队列，默认 Integer.MAX. Actor会将终止的事务数据放入此队列，并等待存入elasticsearch |
| alpha.feature.akka.transaction.repository.elasticsearch.batchSize | 100    | elasticsearch 批量入库数量                                   |
| alpha.feature.akka.transaction.repository.elasticsearch.refreshTime | 5000   | elasticsearch 定时同步到ES时间                               |
| spring.data.elasticsearch.cluster-name                       |        | ES集群名称                                                   |
| spring.data.elasticsearch.cluster-nodes                      |        | ES节点地址，格式：localhost:9300，多个地址逗号分隔           |

### Elasticsearch 索引
Alpha 会在 Elasticsearch 中创建一个名为 `alpha_global_transaction` 的索引

### 使用 Elasticsearch APIs 查询事务数据

* 查询所有事务

  ```bash
  curl http://localhost:9200/alpha_global_transaction/_search
  ```

* 查询匹配 globalTxId 的事务

  ```bash
  curl -X POST http://localhost:9200/alpha_global_transaction/_search -H 'Content-Type: application/json' -d '
  {
    "query": {
      "bool": {
        "must": [{
          "term": {
            "globalTxId.keyword": "974d089a-5476-48ed-847a-1e338456809b"
          }
        }],
        "must_not": [],
        "should": []
      }
    },
    "from": 0,
    "size": 10,
    "sort": [],
    "aggs": {}
  }'
  ```

* 查询返回 JSON 格式

  ```json
  {
    "took": 17,
    "timed_out": false,
    "_shards": {
      "total": 5,
      "successful": 5,
      "skipped": 0,
      "failed": 0
    },
    "hits": {
      "total": 4874,
      "max_score": 1.0,
      "hits": [{
        "_index": "alpha_global_transaction",
        "_type": "alpha_global_transaction_type",
        "_id": "209791a0-34f4-40da-807e-9c5b8786dd61",
        "_score": 1.0,
        "_source": {
          "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
          "type": "SAGA",
          "serviceName": "alpha-benchmark",
          "instanceId": "alpha-benchmark-127.0.0.1",
          "beginTime": 1563982631298,
          "endTime": 1563982631320,
          "state": "COMMITTED",
          "subTxSize": 3,
          "durationTime": 22,
          "subTransactions": [...],
          "events": [...]
        }
      },{...}]
    }
  }
  ```

* 查询返回 JSON样例

  ```json
  {
    "took": 17,
    "timed_out": false,
    "_shards": {
      "total": 5,
      "successful": 5,
      "skipped": 0,
      "failed": 0
    },
    "hits": {
      "total": 4874,
      "max_score": 1.0,
      "hits": [{
        "_index": "alpha_global_transaction",
        "_type": "alpha_global_transaction_type",
        "_id": "209791a0-34f4-40da-807e-9c5b8786dd61",
        "_score": 1.0,
        "_source": {
          "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
          "type": "SAGA",
          "serviceName": "alpha-benchmark",
          "instanceId": "alpha-benchmark-127.0.0.1",
          "beginTime": 1563982631298,
          "endTime": 1563982631320,
          "state": "COMMITTED",
          "subTxSize": 3,
          "durationTime": 22,
          "subTransactions": [{
            "localTxId": "03fe15b2-a070-4e55-9b5b-801c2181dd0a",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "beginTime": 1563982631308,
            "endTime": 1563982631309,
            "state": "COMMITTED",
            "durationTime": 1
          }, {
            "localTxId": "923f83fd-0bce-4fac-8c89-ecbe7c5e9106",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "beginTime": 1563982631320,
            "endTime": 1563982631320,
            "state": "COMMITTED",
            "durationTime": 0
          }, {
            "localTxId": "95821ce3-2202-4e55-9343-4e6a6519821f",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "beginTime": 1563982631309,
            "endTime": 1563982631309,
            "state": "COMMITTED",
            "durationTime": 0
          }],
          "events": [{
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "createTime": 1563982631298,
            "timeout": 0,
            "type": "SagaStartedEvent"
          }, {
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "03fe15b2-a070-4e55-9b5b-801c2181dd0a",
            "createTime": 1563982631299,
            "compensationMethod": "service a",
            "payloads": "AQE=",
            "retryMethod": "",
            "retries": 0,
            "type": "TxStartedEvent"
          }, {
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "03fe15b2-a070-4e55-9b5b-801c2181dd0a",
            "createTime": 1563982631301,
            "type": "TxEndedEvent"
          }, {
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "95821ce3-2202-4e55-9343-4e6a6519821f",
            "createTime": 1563982631302,
            "compensationMethod": "service b",
            "payloads": "AQE=",
            "retryMethod": "",
            "retries": 0,
            "type": "TxStartedEvent"
          }, {
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "95821ce3-2202-4e55-9343-4e6a6519821f",
            "createTime": 1563982631304,
            "type": "TxEndedEvent"
          }, {
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "923f83fd-0bce-4fac-8c89-ecbe7c5e9106",
            "createTime": 1563982631309,
            "compensationMethod": "service c",
            "payloads": "AQE=",
            "retryMethod": "",
            "retries": 0,
            "type": "TxStartedEvent"
          }, {
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "923f83fd-0bce-4fac-8c89-ecbe7c5e9106",
            "createTime": 1563982631311,
            "type": "TxEndedEvent"
          }, {
            "serviceName": "alpha-benchmark",
            "instanceId": "alpha-benchmark-127.0.0.1",
            "globalTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "parentTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "localTxId": "209791a0-34f4-40da-807e-9c5b8786dd61",
            "createTime": 1563982631312,
            "type": "SagaEndedEvent"
          }]
        }
      }]
    }
  }
  ```

  更多用法参考 [Elasticsearch APIs](https://www.elastic.co/guide/en/elasticsearch/reference/6.6/docs.html) 

## 高可用

可以通过部署 Alpha 集群实现服务的高可用，你可以通过参数自己选择事件通道的类型

### 事件通道类型

Alpha 收到 Omeag 发送的事件后放入事件通道等待Akka处理

| 通道类型             | 模式 | 说明                                                         |
| -------------------- | ---- | ------------------------------------------------------------ |
| memory（默认）       | 单例 | 使用内存作为数据通道，不建议在生产环境使用                   |
| redis（coming soon） | 主从 | 使用 Redis PUB/SUB 作为数据通道，集群中的主节点负责处理数据，从节点处于就绪状态，主节点宕机后从节点接管主节点 |
| kafka（coming soon） | 集群 | 使用 Kafka 作为数据通道，使用全局事务ID作为分区策略，集群中的所有节点同时工作，可水平扩展 |


 可以使用参数 `alpha.feature.akka.channel.type` 配置通道类型

* Memory channel

| 参数名                                 | 默认值 | 说明                                        |
| -------------------------------------- | ------ | ------------------------------------------- |
| alpha.feature.akka.channel.type        | memory | 可选类型有 activemq, kafka, redis           |
| alpha.feature.akka.channel.memory.size | -1     | momory类型时内存队列大小，-1表示Integer.MAX |

* Redis channel

  coming soon

* Kafka channel

  coming soon

### Akka 参数配置

可以通过 `akkaConfig.{akka_key} = value` 方式配置Akka参数，例如系统默认的基于内存模式的配置

### Akka 持久化

```properties
akkaConfig.akka.persistence.journal.plugin=akka.persistence.journal.inmem
akkaConfig.akka.persistence.journal.leveldb.dir=target/example/journal
akkaConfig.akka.persistence.snapshot-store.plugin=akka.persistence.snapshot-store.local
akkaConfig.akka.persistence.snapshot-store.local.dir=target/example/snapshots
```

你可以通过参数配置成基于 Redis 的持久化方式

```properties
akkaConfig.akka.persistence.journal.plugin=akka-persistence-redis.journal
akkaConfig.akka.persistence.snapshot-store.plugin=akka-persistence-redis.snapshot
akkaConfig.akka-persistence-redis.redis.mode=simple
akkaConfig.akka-persistence-redis.redis.host=localhost
akkaConfig.akka-persistence-redis.redis.port=6379
akkaConfig.akka-persistence-redis.redis.database=0
```

更多参数请参考 [akka-persistence-redis](https://index.scala-lang.org/safety-data/akka-persistence-redis/akka-persistence-redis/0.4.0?target=_2.11)

你可以在 Alpha 的启动命令中直接设置这些参数，例如

```bash
java -jar alpha-server-${version}-exec.jar \
  --spring.datasource.url=jdbc:postgresql://0.0.0.0:5432/saga?useSSL=false \
  --spring.datasource.username=saga-user \
  --spring.datasource.password=saga-password \
  --spring.profiles.active=prd \
  --alpha.feature.akka.enabled=true \
  --alpha.feature.akka.transaction.repository.type=elasticsearch \
  --spring.data.elasticsearch.cluster-name=docker-cluster \
  --spring.data.elasticsearch.cluster-nodes=localhost:9300 \
  --akkaConfig.akka.persistence.journal.plugin=akka-persistence-redis.journal \
  --akkaConfig.akka.persistence.snapshot-store.plugin=akka-persistence-redis.snapshot \
  --akkaConfig.akka-persistence-redis.redis.mode=simple \
  --akkaConfig.akka-persistence-redis.redis.host=localhost \
  --akkaConfig.akka-persistence-redis.redis.port=6379 \
  --akkaConfig.akka-persistence-redis.redis.database=0  
```

### Akka 集群

coming soon

## 附录

[设计文档](design_fsm_zh.md)

[基准测试报告](benchmark_zh.md)