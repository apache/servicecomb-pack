# Alpha Benchmark tool

The Alpha Benchmark Project is a Stress test tool, Test Alpha speed by simulating sending Omega events. This simulator sends a set of global transactions with three sub-transactions

```prop
SagaStartedEvent
TxStartedEvent
TxEndedEvent
TxStartedEvent
TxEndedEvent
TxStartedEvent
TxEndedEvent
SagaEndedEvent
```

## Basic Usage

```bash
java -jar alpha-benchmark-0.5.0-SNAPSHOT-exec.jar --alpha.cluster.address=0.0.0.0:8080 --w=50 --n=50000 --c=200
```

Output:

```bash
2019-07-15 15:30:53.754  INFO 53871 --- [           main] o.a.s.p.omega.spring.OmegaSpringConfig   : alpha.feature.akka.enabled=false
2019-07-15 15:30:53.819  INFO 53871 --- [           main] o.a.s.p.omega.spring.OmegaSpringConfig   : Discovery alpha cluster address 0.0.0.0:8080 from DEFAULT
Benchmarking ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░

Concurrency Level         150
Time taken for tests      50 seconds
Complete requests         50000
Failed requests           0
Requests per second       1123 [#/sec]
Time per request          150 [ms]

Percentage of the requests served within a certain time (ms)
50%   163.27
60%   161.19
70%   156.95
80%   153.10
90%   150.92
100%  150.17
2019-07-15 15:31:44.654  INFO 53871 --- [           main] o.a.s.p.a.benchmark.SagaEventBenchmark   : OK
```

## Command Line Options

```bash
  --n requests        Number of requests to perform
  --c concurrency     Number of multiple requests to make at a time
  --w warm-up         Number of multiple warm-up to make at a time
```

## Benchmarking Tips

Tuning Java Virtual Machines

```bash
java \
  -Xmx8g -Xms8g -Xmn4g \
  -Xss256k \
  -XX:PermSize=128m -XX:MaxPermSize=512m \
  -XX:+UseConcMarkSweepGC \
  -XX:+UseParNewGC \
  -XX:MaxTenuringThreshold=15 \
  -XX:+ExplicitGCInvokesConcurrent \
  -XX:+CMSParallelRemarkEnabled \
  -XX:SurvivorRatio=8 \
  -XX:+UseCompressedOops \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9090 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -jar alpha-server-0.5.0-SNAPSHOT-exec.jar \
  --spring.datasource.username=saga-user \
  --spring.datasource.password=saga-password \
  --spring.datasource.url="jdbc:postgresql://0.0.09.0:5432/saga?useSSL=false" \
  --spring.profile.active=prd \
  --alpha.feature.akka.enabled=true
```

Optimizing System Performance

```bash
ulimit -u unlimited
ulimit -n 90000
```

