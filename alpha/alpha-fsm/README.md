# Saga State Machine Module

基于状态机控制事务的状态转换，**这是一个试验性模块**
[更多详细内容](https://github.com/apache/servicecomb-pack/blob/SCB-1321/docs/fsm/design_fsm_zh.md)

## Clone & Build

```bash
git clone -b SCB-1321 git@github.com:apache/servicecomb-pack.git
mvn clean install -DskipTests=true -Pdemo,spring-boot-2
```

## Unit Tests

```bash
cd alpha
mvn test -pl alpha-fsm 
```

## Acceptance Tests

```bash
mvn clean verify -f acceptance-tests -pl acceptance-pack-akka-spring-demo -Ddocker.useColor=true -Ddocker.showLogs
```

## Enabled Saga State Machine Module

Using `alpha.feature.akka.enabled=true` launch Alpha and Omega Side 

```properties
alpha.feature.akka.enabled=true
```

## WIP

### Alpha

- [x]  State machine design document
- [x]  State machine prototype
- [x]  State machine prototype unit test
- [x]  Receive saga events using the internal message bus
- [x]  State machine integration test
- [x]  Enable state machine support via parameters 
- [x]  Verify Akka persistent 
- [ ]  Verify Akka cluster reliability
- [ ]  Save the terminated transaction data to the database
- [ ]  Support for in-process nested global transactions
- [ ]  Support for cross-process nested global transactions
- [ ]  Support for query terminated transaction data by RESTful API
- [ ]  Support for query running transaction data by RESTful API
- [ ]  Support for query running transaction data by RESTful API
- [ ]  Support for query suspended global transaction by RESTful API
- [ ]  Support for compensate failed sub-transaction by RESTful API

### Omega Components
- [x]  Enable state machine support via parameters
- [x]  State machine calls omega side compensation
- [x]  @SagaStart supports thread termination after the timeout

### Alpha & Omega
- [x]  Acceptance-pack-akka-spring-demo pass
- [ ]  Add sub-transaction timeout exception for akka acceptance test
- [ ]  Add compensation failure for akka acceptance test
- [ ]  Add compensation retry success for akka acceptance test 


