# Pack 用户指南
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
$ git clone https://github.com/apache/servicecomb-pack.git
$ cd servicecomb-pack
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
### 引入Pack的依赖
```xml
    <dependency>
      <groupId>org.apache.servicecomb.pack</groupId>
      <artifactId>omega-spring-starter</artifactId>
      <version>${pack.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.servicecomb.pack</groupId>
      <artifactId>omega-transport-resttemplate</artifactId>
      <version>${pack.version}</version>
    </dependency>
```
**注意**: 请将`${pack.version}`更改为实际的版本号。
**版本迁移提示**: 从0.3.0 开始，整个项目的代码库名由servicecomb-saga改名为servicecomb-pack, 与此同时我们也更新了对应发布包的组名以及相关包名。
如果你的项目是从saga 0.2.x 迁移过来，请按照下表所示进行修改。 

|  name    |  0.2.x     |  0.3.x    |
| ---- | ---- | ---- |
|  groupId    | org.apache.servicecomb.saga     |  org.apache.servicecomb.pack   |
| Package Name | org.apache.servicecomb.saga     |  org.apache.servicecomb.pack   |


### Saga 支持 
添加Saga的注解及相应的补偿方法
以一个转账应用为例：
1. 在应用入口添加 `@EnableOmega` 的注解来初始化omega的配置并与alpha建立连接。
   ```java
   import org.apache.servicecomb.pack.omega.spring.EnableOmega;
   import org.springframework.boot.autoconfigure.SpringBootApplication;
   
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
   import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;

   @SagaStart(timeout=10)
   public boolean transferMoney(String from, String to, int amount) {
     transferOut(from, amount);
     transferIn(to, amount);
   }
   ```
   **注意:** 默认情况下，超时设置需要显式声明才生效。

3. 在子事务处添加 `@Compensable` 的注解并指明其对应的补偿方法。
   ```java
   import javax.transaction.Transactional;
   import org.apache.servicecomb.pack.omega.transaction.annotations.Compensable;
   
   @Compensable(timeout=5, compensationMethod="cancel")
   @Transactional
   public boolean transferOut(String from, int amount) {
     repo.reduceBalanceByUsername(from, amount);
   }
 
   @Transactional
   public boolean cancel(String from, int amount) {
     repo.addBalanceByUsername(from, amount);
   }
   ```

   **注意:** 实现的服务使用相当的参数，实现的服务和补偿必须满足幂等的条件，同时建议使用Spring @Transactional标注提供本地事务保证。

   **注意:** 默认情况下，超时设置需要显式声明才生效。

   **注意:** 若全局事务起点与子事务起点重合，需同时声明 `@SagaStart` 和 `@Compensable` 的注解。

4. 对转入服务重复第三步即可。

5. 从pack-0.3.0开始, 你可以在服务函数或者取消函数中通过访问 [OmegaContext](https://github.com/apache/servicecomb-pack/blob/master/omega/omega-context/src/main/java/org/apache/servicecomb/saga/omega/context/OmegaContext.java) 来获取 gloableTxId 以及 localTxId 信息。

### TCC 支持
在对应的方法中添加TccStart 和 Participate标注 
 以一个转账应用为例：
1. 在应用入口添加 `@EnableOmega` 的注解来初始化omega的配置并与alpha建立连接。
   ```java
    import org.apache.servicecomb.pack.omega.spring.EnableOmega;
    import org.springframework.boot.autoconfigure.SpringBootApplication;   
   
    @SpringBootApplication
    @EnableOmega
    public class Application {
      public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
      }
    }
   ```
   
2. 在全局事务的起点添加 `@TccStart` 的注解。
    ```java
    import org.apache.servicecomb.pack.omega.context.annotations.TccStart;
        
    @TccStart
    public boolean transferMoney(String from, String to, int amount) {
      transferOut(from, amount);
      transferIn(to, amount);
    }
    ```
    **Note:** 当前TCC还不支持Timeout

3. 在子事务尝试方法处添加 `@Participate` 的注解并指明其对应的执行以及补偿方法名, 
    ```java
    import javax.transaction.Transactional;
    import org.apache.servicecomb.pack.omega.transaction.annotations.Participate;
      
    @Participate(confirmMethod = "confirm", cancelMethod = "cancel")
    @Transactional
    public void transferOut(String from, int amount) {
      // check banalance
    }

    @Transactional
    public void confirm(String from, int amount) {
      repo.reduceBalanceByUsername(from, amount);
    }
    
    @Transactional
    public void cancel(String from, int amount) {
      repo.addBalanceByUsername(from, amount);
    }
    ```

    **Note:** The confirm and cancel method should have same arguments with participate method, confirm and cancel method implemented by services must be idempotent. We highly recommend to use the Spring @Transactional to guarantee the local transaction.
   
    **Note:** 若全局事务起点与子事务起点重合，需同时声明 `@TccStart`  和 `@Participate` 的注解。 

4. 对转入服务重复第三步即可。

5. 从pack-0.3.0开始, 你可以在服务函数或者取消函数中通过访问 [OmegaContext](https://github.com/apache/servicecomb-pack/blob/master/omega/omega-context/src/main/java/org/apache/servicecomb/saga/omega/context/OmegaContext.java) 来获取 gloableTxId 以及 localTxId 信息。


## 如何运行
1. 运行postgreSQL,
   ```bash
   docker run -d -e "POSTGRES_DB=saga" -e "POSTGRES_USER=saga" -e "POSTGRES_PASSWORD=password" -p 5432:5432 postgres
   ```
   如果你想使用MySQL做为后台数据库，可以参考 [此文档](https://github.com/apache/servicecomb-pack/blob/master/docs/faq/en/how_to_use_mysql_as_alpha_backend_database.md)。


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

然后就可以运行相关的微服务了，可通过访问http://${alpha-server:port}/saga/events 来获取所有的saga事件信息。

### 注册中心支持

支持Alpha启动时自动注册到注册中心，Omega通过注册中心获取Alpha的实例列表和gRPC地址

#### Spring Cloud Eureka支持

1. 编译Alpha的Eureka的版本

   在编译时增加`spring-cloud-eureka`参数

   ```bash
   git clone https://github.com/apache/servicecomb-pack.git
   cd servicecomb-pack
   mvn clean install -DskipTests=true -Pspring-cloud-eureka
   ```

   **注意:** 默认情况下，编译的版本兼容spring boot 2.x，当使用omega的项目是基于spring boot 1.x时请在编译的时使用`-Pspring-cloud-eureka,spring-boot-1`参数

   

2. 运行alpha

   运行是增加`spring.profiles.active=spring-cloud-eureka`参数

   ```bash
   java -Dspring.profiles.active=prd -D"spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar --spring.profiles.active=spring-cloud-eureka
   ```

3. 验证是否注册成功

   访问Eureka的注册实例查询接口`curl http://127.0.0.1:8761/eureka/apps/`可以看到如下注册信息，在你metadata中可以看到Alpha的gRPC访问地址`<servicecomb-alpha-server>0.0.0.0:8080</servicecomb-alpha-server>`已经注册

   ```xml
   <applications>
     <versions__delta>1</versions__delta>
     <apps__hashcode>UP_1_</apps__hashcode>
     <application>
       <name>SERVICECOMB-ALPHA-SERVER</name>
       <instance>
         <instanceId>0.0.0.0::servicecomb-alpha-server:8090</instanceId>
         <hostName>0.0.0.0</hostName>
         <app>SERVICECOMB-ALPHA-SERVER</app>
         <ipAddr>0.0.0.0</ipAddr>
         <status>UP</status>
   	  ...
         <metadata>
           <management.port>8090</management.port>
           <servicecomb-alpha-server>0.0.0.0:8080</servicecomb-alpha-server>
         </metadata>
         ...
       </instance>
     </application>
   </applications>
   ```

   **注意:** 默认情况下注册的服务名是`SERVICECOMB-ALPHA-SERVER`,如果你需要自定义服务名可以在运行Alpha的时候通过命令行参数`spring.application.name=XXX`指定

4. 配置omega

   在项目中引入依赖包`omega-spring-cloud-starter`

   ```xml
   <dependency>
   	<groupId>org.apache.servicecomb.pack</groupId>
   	<artifactId>omega-spring-cloud-starter</artifactId>
   	<version>${pack.version}</version>
   </dependency>
   ```

   在 `application.yaml` 添加下面的配置项：

   ```yaml
   alpha:
     cluster:
       register:
         type: spring-cloud
   omega:
     instance:
       instanceId: ${spring.application.name}-${spring.cloud.client.hostname}-${server.port}
   ```

   **注意:** 如果你在启动Alpha的时候通过命令行参数`spring.application.name`自定义了服务名，那么那么你需要在Omega中通过参数`alpha.cluster.serviceId`指定这个服务名

