# Saga | [English](README.md) [![Build Status](https://travis-ci.org/apache/incubator-servicecomb-saga.svg?branch=master)](https://travis-ci.org/apache/incubator-servicecomb-saga?branch=master) [![Coverage Status](https://coveralls.io/repos/github/apache/incubator-servicecomb-saga/badge.svg?branch=master)](https://coveralls.io/github/apache/incubator-servicecomb-saga?branch=master) [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
Apache ServiceComb (incubating) Saga 是一个微服务应用的数据最终一致性解决方案。相对于[TCC](http://design.inf.usi.ch/sites/default/files/biblio/rest-tcc.pdf)而言，在try阶段，Saga会直接提交事务，后续rollback阶段则通过反向的补偿操作来完成。

## 特性
* 高可用。支持集群模式。
* 高可靠。所有的事务事件都持久存储在数据库中。
* 高性能。事务事件是通过gRPC来上报的，且事务的请求信息是通过Kyro进行序列化和反序列化的。
* 低侵入。仅需2-3个注解和编写对应的补偿方法即可进行分布式事务。
* 部署简单。可通过Docker快速部署。
* 支持前向恢复（重试）及后向恢复（补偿）。

## 架构
Saga是由 **alpha** 和 **omega**组成，其中：
* alpha充当协调者的角色，主要负责对事务进行管理和协调。
* omega是微服务中内嵌的一个agent，负责对网络请求进行拦截并向alpha上报事务事件。

下图展示了alpha, omega以及微服务三者的关系：
![Saga Pack 架构](docs/static_files/pack.png)

详情可浏览[Saga Pack 设计文档](docs/design_zh.md). 

## 快速入门
详情可浏览[出行预订示例](saga-demo/booking/README.md)。

## 用户指南
如何构建和使用可浏览[用户指南](docs/user_guide_zh.md)。

## 获取最新发行版本

[下载Saga](http://servicecomb.incubator.apache.org/release/saga-downloads/)

## [常见问题](FAQ_ZH.md)

## 联系我们
* [提交issues](https://issues.apache.org/jira/browse/SCB)
* [gitter聊天室](https://gitter.im/ServiceCombUsers/Lobby)
* 邮件列表: [订阅](mailto:dev-subscribe@servicecomb.incubator.apache.org) [浏览](https://lists.apache.org/list.html?dev@servicecomb.apache.org)

## 贡献
详情可浏览[代码提交指南](http://servicecomb.incubator.apache.org/cn/developers/submit-codes/)。

## License
[Apache 2.0 license](https://github.com/apache/incubator-servicecomb-saga/blob/master/LICENSE)。
