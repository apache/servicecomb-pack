# Saga 用户指南
[![EN doc](https://img.shields.io/badge/document-English-blue.svg)](user_guide.md)

## 准备环境
1. 安装[JDK 1.8][jdk]
2. 安装[Maven 3.x][maven]
3. 安装[Docker][docker]

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[maven]: https://maven.apache.org/install.html
[docker]: https://www.docker.com/get-docker

## 编译

获取源码：
```bash
$ git clone https://github.com/apache/incubator-servicecomb-saga.git
$ cd incubator-servicecomb-saga
```

Saga可通过以下任一方式进行构建：
* 只构建可执行文件：
   ```bash
   $ mvn clean install -DskipTests
   ```

* 同时构建可执行文件和docker镜像：
   ```bash
   $ mvn clean install -DskipTests -Pdocker
   ```

* 同时构建可执行文件以及Saga发行包
   ```bash
      $ mvn clean install -DskipTests -Prelease
   ```
   

在执行以上任一指令后，可在`alpha/alpha-server/target/saga/alpha-server-${version}-exec.jar`中找到alpha server的可执行文件。

## 如何使用
### 引入Saga的依赖
```xml
    <dependency>
      <groupId>org.apache.servicecomb.saga</groupId>
      <artifactId>omega-spring-starter</artifactId>
      <version>${saga.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.servicecomb.saga</groupId>
      <artifactId>omega-transport-resttemplate</artifactId>
      <version>${saga.version}</version>
    </dependency>
```
**注意**: 请将`${saga.version}`更改为实际的版本号。

### 添加Saga的注解及相应的补偿方法
以一个转账应用为例：
1. 在应用入口添加 `@EnableOmega` 的注解来初始化omega的配置并与alpha建立连接。
   ```java
   @SpringBootApplication
   @EnableOmega
   public class Application {
     public static void main(String[] args) {
       SpringApplication.run(Application.class, args);
     }
   }
   ```

2. 在全局事务的起点添加 `@SagaStart` 的注解。
   ```java
   @SagaStart(timeout=10)
   public boolean transferMoney(String from, String to, int amount) {
     transferOut(from, amount);
     transferIn(to, amount);
   }
   ```
   **注意:** 默认情况下，超时设置需要显式声明才生效。

3. 在子事务处添加 `@Compensable` 的注解并指明其对应的补偿方法。
   ```java
   @Compensable(timeout=5, compensationMethod="cancel")
   public boolean transferOut(String from, int amount) {
     repo.reduceBalanceByUsername(from, amount);
   }
 
   public boolean cancel(String from, int amount) {
     repo.addBalanceByUsername(from, amount);
   }
   ```

   **注意:** 实现的服务和补偿必须满足幂等的条件。

   **注意:** 默认情况下，超时设置需要显式声明才生效。

   **注意:** 若全局事务起点与子事务起点重合，需同时声明 `@SagaStart` 和 `@Compensable` 的注解。

4. 对转入服务重复第三步即可。

5. 从Saga-0.3.0, 你可以在服务函数或者取消函数中通过访问 [OmegaContext](https://github.com/apache/incubator-servicecomb-saga/blob/master/omega/omega-context/src/main/java/org/apache/servicecomb/saga/omega/context/OmegaContext.java) 来获取 gloableTxId 以及 localTxId 信息。


## 如何运行
1. 运行postgreSQL,
   ```bash
   docker run -d -e "POSTGRES_DB=saga" -e "POSTGRES_USER=saga" -e "POSTGRES_PASSWORD=password" -p 5432:5432 postgres
   ```
   如果你想使用MySQL做为后台数据库，可以参考 [此文档](https://github.com/apache/incubator-servicecomb-saga/blob/master/docs/faq/en/how_to_use_mysql_as_alpha_backend_database.md)。


2. 运行alpha。在运行alpha前，请确保postgreSQL已正常启动。可通过docker或可执行文件的方式来启动alpha。
   * 通过docker运行：
      ```bash
      docker run -d -p 8080:8080 -p 8090:8090 -e "JAVA_OPTS=-Dspring.profiles.active=prd -Dspring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" alpha-server:${saga_version}
      ```
   * 通过可执行文件运行：
      ```bash
      java -Dspring.profiles.active=prd -D"spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar
      ```

   **注意**: 请在执行命令前将`${saga_version}`和`${host_address}`更改为实际值。


   **注意**: 默认情况下，8080端口用于处理omega处发起的gRPC的请求，而8090端口用于处理查询存储在alpha处的事件信息。


3. 配置omega。在 `application.yaml` 添加下面的配置项：
   ```yaml
   spring:
     application:
       name: {application.name}
   alpha:
     cluster:
       address: {alpha.cluster.addresses}
   ```

然后就可以运行相关的微服务了，可通过访问http://${alpha-server:port}/events 来获取所有的saga事件信息。
