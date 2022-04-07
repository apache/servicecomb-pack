# Akka 配置

Alpha 已经定义了一些 Akka 的参数，如果要在外部修改，可以通过 `akkaConfig.{akka key} = value` 方式配置

* Alpha 单例模式

  状态机持久化参数

  ```properties
  akkaConfig:
    akka:
      persistence:
        journal:
          plugin: akka.persistence.journal.inmem
          leveldb.dir: actor/persistence/journal
        snapshot-store:
          plugin: akka.persistence.snapshot-store.local
          local.dir: actor/persistence/snapshots
  ```

* Alpha 集群模式

  状态机持久化参数

  ```properties
  akkaConfig:
    akka:
      actor:
        provider: cluster
      persistence:
        at-least-once-delivery:
          redeliver-interval: 10s
          redelivery-burst-limit: 2000
        journal:
          plugin: akka-persistence-redis.journal
        snapshot-store:
          plugin: akka-persistence-redis.snapshot
  akka-persistence-redis:
    redis:
      mode: "simple"
      host: "127.0.0.1"
      port: 6379
      database: 0        
  ```

  Kafka 消费者参数

  ```properties
  akkaConfig:
    akka:
      kafka:
        consumer:
          poll-interval: 50ms
          stop-timeout: 30s
          close-timeout: 20s
          commit-timeout: 15s
          commit-time-warning: 5s
          commit-refresh-interval: infinite
          wait-close-partition: 500ms
          position-timeout: 10s
          offset-for-times-timeout: 10s
          metadata-request-timeout: 10s
          eos-draining-check-interval: 30ms
          partition-handler-warning: 5s
          connection-checker.enable: false
          connection-checker.max-retries: 3
          connection-checker.check-interval: 15s
          connection-checker.backoff-factor: 2.0
  ```