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
   $ mvn clean install -DskipTests -Pspring-boot-2
   ```

* 同时构建可执行文件和docker镜像：
   ```bash
   $ mvn clean install -DskipTests -Pdocker,spring-boot-2
   ```

* 同时构建可执行文件以及Saga发行包
   ```bash
      $ mvn clean install -DskipTests -Prelease,spring-boot-2
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

## 在Alpha与Omega之间启用SSL

详情请参考[启用 SSL](enable_ssl.md)文档.


## 注册中心支持

支持Alpha启动时注册到发现服务，Omega通过发现服务获取Alpha的实例列表和gRPC地址

### Consul 支持

当前版本支持 Spring Cloud Consul 2.x，你可以使用 `-Pspring-boot-1` 参数重新编译支持 Spring Cloud Consul 1.x 版本

1. 运行alpha

   运行时增加 `spring.cloud.consul.enabled=true` 参数

   ```bash
   java -jar alpha-server-${saga_version}-exec.jar \ 
     --spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false \
     --spring.datasource.username=saga \
     --spring.datasource.password=saga \
     --spring.cloud.consul.enabled=true \
     --spring.cloud.consul.host=${consul_host} \
     --spring.cloud.consul.port=${consul_port} \
     --spring.profiles.active=prd 
   ```

   **注意:** `${consul_host}` 是 consul 地址, `${consul_port}` 是 consul 端口

   **注意:** 更多 Consul 参数请参考 [Spring Cloud Consul 2.x](https://cloud.spring.io/spring-cloud-consul/spring-cloud-consul.html) [Spring Cloud Consul 1.x](https://cloud.spring.io/spring-cloud-consul/1.3.x/single/spring-cloud-consul.html)

2. 验证是否注册成功

   访问 Consul 的注册实例查询接口`curl http://127.0.0.1:8500/v1/agent/services`可以看到如下注册信息，在你 Tags 中可以看到 Alpha 的 gRPC 访问地址已经注册

   ```json
   {
       "servicecomb-alpha-server-0-0-0-0-8090": {
           "ID": "servicecomb-alpha-server-0-0-0-0-8090",
           "Service": "servicecomb-alpha-server",
           "Tags": [
               "alpha-server-host=0.0.0.0",
               "alpha-server-port=8080",
               "secure=false"
           ],
           "Meta": {},
           "Port": 8090,
           "Address": "10.50.7.14",
           "Weights": {
               "Passing": 1,
               "Warning": 1
           },
           "EnableTagOverride": false
       }
   }
   ```

   **注意:** 默认情况下注册的服务名是`servicecomb-alpha-server`,如果你需要自定义服务名可以在运行Alpha的时候通过命令行参数`spring.application.name`配置

3. 配置omega

   在项目中引入依赖包 `omega-spring-cloud-consul-starter`

   ```xml
   <dependency>
   	<groupId>org.apache.servicecomb.pack</groupId>
   	<artifactId>omega-spring-cloud-consul-starter</artifactId>
   	<version>${pack.version}</version>
   </dependency>
   ```

   在 `application.yaml` 添加下面的配置项：

   ```yaml
   spring:
     cloud:
       consul:
         discovery:
         	register: false
         host: 127.0.0.1
         port: 8500
         
   alpha:
     cluster:
       register:
         type: consul
   ```

   - `spring.cloud.consul.host` 配置 Consul 注册中心的地址，`spring.cloud.consul.port` 配置 Consul 注册中心的端口，`spring.cloud.consul.discovery.register=false` 表示不注册自己到注册中心，更多 Consul 客户端配置可以参考[Spring Cloud Consul 2.x](https://cloud.spring.io/spring-cloud-consul/spring-cloud-consul.html) [Spring Cloud Consul 1.x](https://cloud.spring.io/spring-cloud-consul/1.3.x/single/spring-cloud-consul.html)
   - `alpha.cluster.register.type=consul` 配置Omega获取Alpha的方式是通过 Consul 的注册中心

   **注意:** 如果你在启动Alpha的时候通过命令行参数`spring.application.name`自定义了服务名，那么你需要在Omega中通过参数`alpha.cluster.serviceId`指定这个服务名

### Spring Cloud Eureka支持

当前版本支持 Spring Cloud Netflix 2.x，你可以使用 `-Pspring-boot-1` 参数重新编译支持 Spring Cloud Netflix 1.x 版本

1. 编译 eureka 的版本

   使用 `-Pspring-cloud-eureka` 参数编译支持 eureka 的版本

   ```bash
   git clone https://github.com/apache/servicecomb-pack.git
   cd servicecomb-pack
   mvn clean install -DskipTests=true -Pspring-boot-2,spring-cloud-eureka
   ```

2. 运行alpha

   运行时增加 `eureka.client.enabled=true` 参数

   ```bash
   java -jar alpha-server-${saga_version}-exec.jar \ 
     --spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false \
     --spring.datasource.username=saga \
     --spring.datasource.password=saga \
     --eureka.client.enabled=true \
     --eureka.client.service-url.defaultZone=http://127.0.0.1:8761/eureka \  
     --spring.profiles.active=prd 
   ```

   **注意:** 更多 eureka 参数请参考 [Spring Cloud Netflix 2.x](https://cloud.spring.io/spring-cloud-netflix/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter) [Spring Cloud Netflix 1.x](https://cloud.spring.io/spring-cloud-netflix/1.4.x/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter)

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

   **注意:** 默认情况下注册的服务名是`SERVICECOMB-ALPHA-SERVER`,如果你需要自定义服务名可以在运行Alpha的时候通过命令行参数`spring.application.name`配置

4. 配置omega

   在项目中引入依赖包 `omega-spring-cloud-eureka-starter`

   ```xml
   <dependency>
   	<groupId>org.apache.servicecomb.pack</groupId>
   	<artifactId>omega-spring-cloud-eureka-starter</artifactId>
   	<version>${pack.version}</version>
   </dependency>
   ```

   在 `application.yaml` 添加下面的配置项：

   ```yaml
   eureka:
     client:
       service-url:
         defaultZone: http://127.0.0.1:8761/eureka
   alpha:
     cluster:
       register:
         type: eureka
   ```

   * `eureka.client.service-url.defaultZone` 配置Eureka注册中心的地址，更多Eureka客户端配置可以参考[Spring Cloud Netflix 2.x](https://cloud.spring.io/spring-cloud-netflix/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter) 或 [Spring Cloud Netflix 1.x](https://cloud.spring.io/spring-cloud-netflix/1.4.x/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter)
   * `alpha.cluster.register.type=eureka` 配置Omega获取Alpha的方式是通过Eureka的注册中心

   **注意:** 如果你在启动Alpha的时候通过命令行参数`spring.application.name`自定义了服务名，那么你需要在Omega中通过参数`alpha.cluster.serviceId`指定这个服务名

## 集群

Alpha 可以通过部署多实例的方式保证高可用，使用 `alpha.cluster.master.enabled=true` 参数开启集群支持