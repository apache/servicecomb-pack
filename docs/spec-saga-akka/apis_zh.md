# APIs

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

- eventReceived: Alpha 收到的 gRPC 事件数量
- eventAccepted:  Alpha 处理的 gRPC 事件数量（事件放入事件通道）
- eventRejected:  Alpha 拒绝的 gRPC 事件数量
- eventAvgTime: Alpha 平均耗时（毫秒）
- actorReceived: Akka 收到的事件数量
- actorAccepted:  Akka 处理的事件数量
- actorRejected: Akka 拒绝的事件数量
- actorAvgTime: Akka 平均耗时（毫秒）
- sagaBeginCounter: 开始的 Saga 全局事务数量
- sagaEndCounter: 结束的 Saga 全局事务数量
- sagaAvgTime: 平均耗时（毫秒）
- committed: COMMITTED状态的 Saga 全局事务数量
- compensated: COMPENSATED状态的 Saga 全局事务数量
- suspended: SUSPENDED状态的 Saga 的全局事务数量
- repositoryReceived: 存储模块收到的全局事务数量
- repositoryAccepted: 存储模块处理的全局事务数量
- repositoryRejected: 存储模块拒绝的全局事务数量
- repositoryAvgTime: 平均耗时（毫秒）

#### 事务数据查询

> 需要启动 Elasticsearch 存储事务

- 查询事务列表

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

  - page 页号
  - size 返回行数

  返回参数

  - total 总行数
  - page 本次查询结果页号
  - size 本次查询行数
  - elapsed 本次查询耗时（毫秒）
  - globalTransactions 事件数据列表

- 查询一条事务

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

  - globalTxId 全局事务ID

  返回参数

  - globalTxId 全局事务ID
  - type 事务类型，目前只有SAGA，后期增加TCC
  - serviceName 全局事务发起方服务名称
  - instanceId 全局事务发起方实例ID
  - beginTime 事务开始时间
  - endTime 事务结束时间
  - state 事务最终状态
  - subTxSize 包含子事务数量
  - durationTime 全局事务处理耗时
  - subTransactions 子事务数据列表
  - events 事件列表