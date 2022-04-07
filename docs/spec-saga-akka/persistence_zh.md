# 事务数据持久化

只有结束的事务才会被持久化到 Elasticsearch，执行中的事务数据通过Akka持久化。事务结束状态有以下几种

- 事务成功结束，最后状态为 COMMITTED

- 事务补偿后结束，最后状态为 COMPENSATED

- 事务异常结束，最后状态为 SUSPENDED

  导致事务异常结束有以下几种情况

  1. 事务超时
  2. Alpha收到了不符合预期的事件，例如在 收到 TxStartedEvent 前就收到了 TxEndedEvent，或者没有收到任何子事务事件就收到了 SagaEndedEvent等，这些规则都被定义在了有限状态机中。

### 持久化参数

| 参数名                                                         | 默认值 | 说明                      |
|---------------------------------------------------------------|-------|-------------------------|
| alpha.spec.saga.akka.repository.name                          |       | 持久化类型，目前可选值 elasticsearch，如果不设置则不存储 |
| alpha.spec.saga.akka.repository.elasticsearch.batch-size      | 100   | elasticsearch 批量入库数量    |
| alpha.spec.saga.akka.repository.elasticsearch.refresh-time    | 5000  | elasticsearch 定时同步到ES时间 |
| alpha.spec.saga.akka.repository.elasticsearch.uris            |       | ES节点地址，格式：http://localhost:9200，多个地址逗号分隔 |

### Elasticsearch 索引

Alpha 会在 Elasticsearch 中创建一个名为 `alpha_global_transaction` 的索引

### 使用 Elasticsearch APIs 查询事务数据

- 查询所有事务

  ```bash
  curl http://localhost:9200/alpha_global_transaction/_search
  ```

- 查询匹配 globalTxId 的事务

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

- 查询返回 JSON 格式

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

- 查询返回 JSON样例

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

  更多用法参考 [Elasticsearch 7.X APIs](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/docs.html) 