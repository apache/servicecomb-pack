# Saga User Guide
[![EN doc](https://img.shields.io/badge/document-English-blue.svg)](user_guide.md)

## 准备环境
1. 安装[JDK 1.8][jdk]
2. 安装[Maven 3.x][maven]
3. 安装[Docker][docker]

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[maven]: https://maven.apache.org/install.html
[docker]: https://www.docker.com/get-docker

## 编译
编译Saga，只需以下几步：
```bash
$ git clone https://github.com/apache/incubator-servicecomb-saga.git
$ cd incubator-servicecomb-saga
$ mvn clean install -DskipTests -Pdocker
```

## 如何使用
### 引入Saga的依赖
```xml
    <dependency>
      <groupId>org.apache.servicecomb.saga</groupId>
      <artifactId>omega-spring-starter</artifactId>
      <version>0.0.3-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>org.apache.servicecomb.saga</groupId>
      <artifactId>omega-transport-resttemplate</artifactId>
      <version>0.0.3-SNAPSHOT</version>
    </dependency>
```

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
   **注意:** 默认情况下，超时设置需要显式声明才生效。

   **注意:** 若全局事务起点与子事务起点重合，需同时声明 `@SagaStart` 和 `@Compensable` 的注解。

4. 对转入服务重复第三步即可。

## 如何运行
1. 运行postgreSQL
   ```bash
   docker run -d -e "POSTGRES_DB=saga" -e "POSTGRES_USER=saga" -e "POSTGRES_PASSWORD=password" -p 5432:5432 postgres
   ```

2. 运行alpha。在运行alpha前，请确保postgreSQL已正常启动。
   ```bash
   docker run -d -p 8090:8090 \
     -e "JAVA_OPTS=-Dspring.profiles.active=prd" \
     -e "spring.datasource.url=jdbc:postgresql://{docker.host.address}:5432/saga?useSSL=false" \
     alpha-server:0.0.3-SNAPSHOT
   ```

3. 配置omega。在 `application.yaml` 添加下面的配置项：
   ```yaml
   spring:
     application:
       name: {application.name}
   alpha:
     cluster:
       address: {alpha.cluster.addresses}
   ```

然后就可以运行相关的微服务了。
