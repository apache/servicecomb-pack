# Roadmap
- epic means it's big task, we need to break it know into small tasks
- new means it's smaller tasks for newbee to know better about the code base.
- hard means it's a hard core which needs lots efforts

## Saga
- [] [SCB-16](https://issues.apache.org/jira/browse/SCB-16) Using MQ to increase the availability (epic)
- [] [SCB-878](https://issues.apache.org/jira/browse/SCB-878) Pack Performance Tuning (epic)

### DEMO

### Test
- [] [SCB-668](https://issues.apache.org/jira/browse/SCB-668) Using docker-compose file to start Services from docker plugin in the Accept test(new)
- [] [SCB-306](https://issues.apache.org/jira/browse/SCB-306) Simulate different recover use cases on the alpha side
- [] [SCB-240](https://issues.apache.org/jira/browse/SCB-240) Performance impacts test (new)

### centric-saga
- [] [SCB-734](https://issues.apache.org/jira/browse/SCB-734) Provide DSL support for building the Saga invocations (epic)

### Pack
- [] [SCB-976](https://issues.apache.org/jira/browse/SCB-976) Create new git repo for ServiceComb Saga Pack module

#### Omega
- [] [SCB-170](https://issues.apache.org/jira/browse/SCB-170) Separate serialization from grpc to reuse the same stream (new++)
- [] [SCB-275](https://issues.apache.org/jira/browse/SCB-275) Refactoring Retry logical (new)
- [] [SCB-258](https://issues.apache.org/jira/browse/SCB-258) Idempotent support (new)
- [] [SCB-163](https://issues.apache.org/jira/browse/SCB-163) Share the context cross the thread (hard)
- [] [SCB-803](https://issues.apache.org/jira/browse/SCB-803) Add hop id to specify the transaction relationship
- [] [SCB-999](https://issues.apache.org/jira/browse/SCB-999) Omega load the Alpha Cluster address dynamically
- [] [SCB-1000](https://issues.apache.org/jira/browse/SCB-1000) Implement the timeout monitor on the Omega side

### Alpha
- [] [SCB-341](https://issues.apache.org/jira/browse/SCB-341) Multi tenant support (new)
- [] [SCB-1005](https://issues.apache.org/jira/browse/SCB-1005) HA solution of the Alpha EventScanner (new) 
- [] [SCB-241](https://issues.apache.org/jira/browse/SCB-241) Visual Transactions, auditing mode support (new)
- [] [SCB-557](https://issues.apache.org/jira/browse/SCB-557) Watch mode of Alpha (new)
