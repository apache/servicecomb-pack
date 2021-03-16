# Pack | [English](README.md) [![Build Status](https://github.com/apache/servicecomb-pack/actions/workflows/master-push-build.yaml/badge.svg?branch=master)](https://github.com/apache/servicecomb-pack/actions/workflows/master-push-build.yaml?query=branch%3Amaster) [![Coverage Status](https://coveralls.io/repos/github/apache/servicecomb-pack/badge.svg?branch=master)](https://coveralls.io/github/apache/servicecomb-pack?branch=master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.servicecomb.pack/pack/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Corg.apache.servicecomb.pack) [![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=servicecomb-pack&metric=alert_status)](https://sonarcloud.io/dashboard?id=servicecomb-pack) [![Gitter](https://img.shields.io/badge/ServiceComb-Gitter-ff69b4.svg)](https://gitter.im/ServiceCombUsers/Saga)
Apache ServiceComb Pack 是一个微服务应用的数据最终一致性解决方案。


## 关键特性
* 高可用：支持高可用的集群模式部署。
* 高可靠：所有的关键事务事件都持久化存储在数据库中。
* 高性能：事务事件是通过高性能gRPC来上报的，且事务的请求和响应消息都是通过Kyro进行序列化和反序列化。
* 低侵入：仅需2-3个注解和编写对应的补偿方法即可引入分布式事务。
* 部署简单：支持通过容器（Docker）进行快速部署和交付。
* 补偿机制灵活：支持前向恢复（重试）及后向恢复（补偿）功能。
* 扩展简单：基于Pack架构很容实现多种协调协议，目前支持TCC、Saga协议，未来还可以添加其他协议支持。


## 架构
ServiceComb Pack 架构是由 **alpha** 和 **omega**组成，其中：
* alpha充当协调者的角色，主要负责对事务进行管理和协调。
* omega是微服务中内嵌的一个agent，负责对调用请求进行拦截并向alpha上报事务事件。

下图展示了alpha, omega以及微服务三者的关系：
![ServiceComb Pack 架构](docs/static_files/pack.png)
在此架构基础上我们除了实现saga协调协议以外，还实现了TCC协调协议。
详情可浏览[ServiceComb Pack 设计文档](docs/design_zh.md).

同时社区也提供了多种语言的Omega实现:
* Go语言版本Omega 可参见 https://github.com/jeremyxu2010/matrix-saga-go
* C#语言版本Omega 可参见 https://github.com/OpenSagas-csharp/servicecomb-saga-csharp


## 快速入门
* Saga在ServiceComb Java Chassis的应用可以参考[出行预订](demo/saga-servicecomb-demo/README.md)
* Saga在Spring应用的用法可参考[出行预订示例](demo/saga-spring-demo/README.md)。
* Saga在Dubbo应用的用法可参考[Dubbo示例](demo/saga-dubbo-demo/README.md).
* TCC在Spring应用的用法可以参考[TCC示例](demo/tcc-spring-demo/README.md)
* 示例的的调试方法可以参考[调试Spring示例](demo/saga-spring-demo#debugging).


## 编译和运行代码

* 编译代码并且运行相关的单元测试
   ```bash
      $ mvn clean install
   ```
* 编译示例代码，并生成docker镜像（maven会根据是否安装docker来启动这部分的设置），运行验收测试。
   ```bash
      $ mvn clean install -Pdemo
   ```
* 编译示例代码，并生产docker镜像, 不运行测试
   ```bash
      $ mvn clean install -DskipTests=true -Pdemo
   ```
* 编译软件发布包，不运行测试, maven会在distribution/target目录下生成的发布包. 
  ```bash
     $ mvn clean install -DskipTests=true -Prelease
  ```  
          

## 用户指南
如何构建和使用可浏览[用户指南](docs/user_guide_zh.md)。


## 获取最新版本

获取最新发行版本:

* [下载软件包](http://servicecomb.apache.org/release/pack-downloads/)

获取最新预览版本:

*  最新的预览版本会发布到Apache nexus的仓库中，请将如下的仓库描述信息加到你的pom.xml文件中.
   ```
           <repositories>
             <repository>
               <releases />
               <snapshots>
                 <enabled>true</enabled>
               </snapshots>
               <id>repo.apache.snapshot</id>
               <url>https://repository.apache.org/content/repositories/snapshots/</url>
             </repository>
           </repositories>
           <pluginRepositories>
             <pluginRepository>
               <releases />
               <snapshots>
                 <enabled>true</enabled>
               </snapshots>
               <id>repo.apache.snapshot</id>
               <url>https://repository.apache.org/content/repositories/snapshots/</url>
             </pluginRepository>
           </pluginRepositories>

   ```    


## [常见问题](FAQ_ZH.md)


## 联系我们
* [提交issues](https://issues.apache.org/jira/browse/SCB)
* [gitter聊天室](https://gitter.im/ServiceCombUsers/Saga)
* 邮件列表: [订阅](mailto:dev-subscribe@servicecomb.apache.org) [浏览](https://lists.apache.org/list.html?dev@servicecomb.apache.org)

## 项目贡献者
[![贡献者](https://badges.implements.io/api/contributors?org=apache&repo=servicecomb-pack&width=1280&size=48&padding=6&type=jpeg)](https://github.com/apache/servicecomb-pack/graphs/contributors)

## 参与贡献
详情可浏览[代码提交指南](http://servicecomb.apache.org/cn/developers/submit-codes/)。

## License
[Apache 2.0 license](https://github.com/apache/servicecomb-pack/blob/master/LICENSE)。
