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
1. 在全局事务的起点添加 `@SagaStart` 的注解来让Omega创建一个新的全局事务。如果不标注这个事务起点的话，后续子事务在执行过程中会报找不到全局事务ID的错误。
   ```java
   import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;
   
   @SagaStart(timeout=10)
   public boolean transferMoney(String from, String to, int amount) {
     transferOut(from, amount);
     transferIn(to, amount);
   }
   ```
   **注意:** 默认情况下，超时设置需要显式声明才生效。

2. 在子事务处添加 `@Compensable` 的注解并指明其对应的补偿方法。
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

3. 增加 alpha.cluster.address 参数

   ```yaml
   alpha:
     cluster:
       address: alpha-server.servicecomb.io:8080
   ```
4. 增加 omega.spec.names 参数

   ```yaml
   omega:
     spec:
       names: saga
   ```
   
5. 对转入服务重复第二步即可。

6. 从 pack-0.3.0 开始, 你可以在服务函数或者取消函数中通过访问 [OmegaContext](https://github.com/apache/servicecomb-pack/blob/master/omega/omega-context/src/main/java/org/apache/servicecomb/pack/omega/context/OmegaContext.java) 来获取 gloableTxId 以及 localTxId 信息。

7. 从 pack-0.7.0 开始, 你可以通过 alpha.spec.names 参数改变分布式事务规格, 目前支持的模式有 saga-db(默认), tcc-db, saga-akka

#### <a name="explicit-tx-context-passing"></a>显式传递事务上下文

在一般情况下，Omega能够替你处理事务上下文的传递工作（详情见[服务间通信流程](design_zh.md#comm)），因此你的代码并不需要知道事务上下文的存在。而事务上下文的传递实际上是通过在请求方注入、在接受方提取来完成的，下面举一个请例子来说明这个过程：

Service A:

```java
@SagaStart
public void foo() {
  restTemplate.postForEntity("http://service-b/bar", ...);
}
```

Service B:

```java
@GetMapping("/bar")
@Compensable
public void bar() {
  ...
}
```

我们可以先来看看Omega是怎么传递事务上下文的：

1. Service A的foo方法会开启一个新的全局事务。
2. [TransactionClientHttpRequestInterceptor][src-TransactionClientHttpRequestInterceptor]会在RestTemplate请求Service B时在Http请求头中注入事务上下文信息。
3. 当Servce B接收到请求时，[TransactionHandlerInterceptor][src-TransactionHandlerInterceptor]会从请求头中提取事务上下文信息。

目前Omega支持以下形式的隐式事务上下文传递：

1. omega-transport-{dubbo,feign,resttemplate,servicecomb}。请注意在你有使用到相关服务调用组件时，请将以上的传输组件添加到你的class path中，否则相关的全局事务ID是无法在服务调用过程中传递。
2. 同线程内调用（基于OmegaContext的ThreadLocal字段）。
3. 标注了@OmegaContextAware的java.util.concurrent.Executor{Service}。

那么问题来了，如果无法隐式传递事务上下文怎么办？比如Service A使用某种RPC机制件来调用Service B，而你又没有办法注入或提取事务上下文信息。这个时候你只能采用显式的方式把事务上下文传递出去。ServiceComb Pack从0.5.0开始提供了两个类来实现这一点。

##### 利用TransactionContext传递

Service A:

```java
@SagaStart
public void foo(BarCommand cmd) {
  TransactionContext localTxContext = omegaContext.getTransactionContext();
  someRpc.send(cmd, localTxContext);
}
```

Service B:

```java
public void listen(BarCommand cmd, TransactionContext injectedTxContext) {
  bar(cmd, injectedTxContext);
}
@Compensable
public void bar(BarCommand cmd, TransactionContext injectedTxContext) {
  ...
  // TransactionContext localTxContext = omegaContext.getTransactionContext();
}
```

需要注意的是`bar`方法接收到的是注入的事务上下文，在进入`bar`之后从OmegaContext得到的是本地事务上下文（Omega替你开启了新的事务）。如果Service B也需要显式地传递事务上下文，那么应该使用本地事务上下文。

##### 利用TransactionContextProperties传递

Service A：

```java
public class BarCommand {}
public class BarCommandWithTxContext 
  extends BarCommand implements TransactionContextProperties {
  // setter getter for globalTxId
  // setter getter for localTxId
}
@SagaStart
public void foo(BarCommand cmd) {
  BarCommandWithTxContext cmdWithTxContext = new BarCommandWithTxContext(cmd);
  cmdWithTxContext.setGlobalTxId(omegaContext.globalTxId());
  cmdWithTxContext.setLocalTxId(omegaContext.localTxId());
  someRpc.send(cmdWithTxContext);
}
```

Service B:

```java
public void listen(BarCommandWithTxContext cmdWithTxContext) {
  bar(cmdWithTxContext);
}

@Compensable
public void bar(BarCommandWithTxContext cmdWithTxContext) {
  ...
  // TransactionContext localTxContext = omegaContext.getTransactionContext();
}
```

和前面一种方式类似，TransactionContextProperties.get{Global,Local}TxId()返回的也是注入的事务上下文信息。

#### 手动结束Saga

从pack-0.5.0开始`@SagaStart`添加了一个属性`autoClose`，该属性用于控制`@SagaStart`所标记的方法执行后是否自动发送SagaEndedEvent（默认值为`true`）。当`autoClose=false`时你需要使用`@SagaEnd`来手动发送`SagaEndedEvent`，比如：

Service A:

```java
@SagaStart(autoClose=false)
public void foo() {
  restTemplate.postForEntity("http://service-b/bar", ...);
}
```

Service B:

```java
@GetMapping("/bar")
@Compensable
@SagaEnd
public void bar() {
  ...
}
```

### TCC 支持
在对应的方法中添加TccStart 和 Participate标注 
 以一个转账应用为例：

1. 在全局事务的起点添加 `@TccStart` 的注解。
   ```java
   import org.apache.servicecomb.pack.omega.context.annotations.TccStart;
       
   @TccStart
   public boolean transferMoney(String from, String to, int amount) {
     transferOut(from, amount);
     transferIn(to, amount);
   }
   ```
   **Note:** 当前TCC还不支持Timeout

2. 在子事务尝试方法处添加 `@Participate` 的注解并指明其对应的执行以及补偿方法名, 
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

    **注意:** `confirm`和`cancel`方法的参数列表应该与`@Participate`方法一样，并且它们必须是幂等。我们强烈建议使用Spring的`@Transactional`来保证本地事务的一致性。

    **注意:** 若全局事务起点与子事务起点重合，需同时声明 `@TccStart`  和 `@Participate` 的注解。 

3. 增加 alpha.cluster.address 参数

    ```yaml
    alpha:
      cluster:
        address: alpha-server.servicecomb.io:8080
    ```

4. 增加 omega.spec.names 参数

   ```yaml
   omega:
     spec:
       names: tcc
   ```
   
5. 对转入服务重复第二步即可。

6. 从pack-0.3.0开始, 你可以在服务函数或者取消函数中通过访问 [OmegaContext](https://github.com/apache/servicecomb-pack/blob/master/omega/omega-context/src/main/java/org/apache/servicecomb/pack/omega/context/OmegaContext.java) 来获取 gloableTxId 以及 localTxId 信息。

#### 显式传递事务上下文

与Saga的`@Compensable`一样，TCC的`@Participate`也支持[显式传递事务上下文](#explicit-tx-context-passing)，详情可参阅Saga章节。


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
   omega:
     spec:
       names: saga
   ```

然后就可以运行相关的微服务了，可通过访问http://${alpha-server:port}/saga/events 来获取所有的saga事件信息。

## 在Alpha与Omega之间启用SSL

详情请参考[启用 SSL](enable_ssl.md)文档.


## 注册中心支持

支持Alpha启动时注册到发现服务，Omega通过发现服务获取Alpha的实例列表和gRPC地址

### Consul 支持

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

   - spring boot 版本兼容

     如果你的项目使用的不是 spring boot 2.3.X 版本，那么请参照此列表增加兼容的spring-cloud-starter-consul-discovery版本

     | spring boot   | spring-cloud-starter-consul-discovery |
     | ------------- | ------------------------------------- |
     | 2.3.12.RELEASE | 2.2.8.RELEASE                        |
     | 2.1.x.RELEASE | 2.1.1.RELEASE                         |
     | 2.0.x.RELEASE | 2.0.2.RELEASE                         |

     ```xml
       <dependencyManagement>
         <dependencies>
           <dependency>
             <groupId>org.springframework.cloud</groupId>
             <artifactId>spring-cloud-starter-consul-discovery</artifactId>
             <version>2.2.8.RELEASE</version>
           </dependency>
         </dependencies>
       </dependencyManagement>
     ```

   **注意:** 如果你在启动Alpha的时候通过命令行参数`spring.application.name`自定义了服务名，那么你需要在Omega中通过参数`alpha.cluster.serviceId`指定这个服务名

### Spring Cloud Eureka支持

1. 编译 eureka 的版本

   使用 `-Pspring-cloud-eureka` 参数编译支持 eureka 的版本

   ```bash
   git clone https://github.com/apache/servicecomb-pack.git
   cd servicecomb-pack
   mvn clean install -DskipTests=true -Pspring-cloud-eureka
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

   * spring boot 版本兼容

     如果你的项目使用的不是 spring boot 2.3.X 版本，那么请参照此列表增加兼容的spring-cloud-starter-consul-discovery版本

     | spring boot   | spring-cloud-starter-netflix-eureka-client |
     |--------------------------------------------| ------------------------------------------ |
     | 2.3.12.RELEASE | 2.2.10.RELEASE                             | 
     | 2.1.x.RELEASE | 2.1.1.RELEASE                              |
     | 2.0.x.RELEASE | 2.0.3.RELEASE                              |

     ```xml
       <dependencyManagement>
         <dependencies>
           <dependency>
             <groupId>org.springframework.cloud</groupId>
             <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
             <version>2.2.10.RELEASE</version>
           </dependency>
         </dependencies>
       </dependencyManagement>
     ```

   **注意:** 如果你在启动Alpha的时候通过命令行参数`spring.application.name`自定义了服务名，那么你需要在Omega中通过参数`alpha.cluster.serviceId`指定这个服务名

### Spring Cloud Zookeeper 支持

1. 运行alpha

   运行时增加 `spring.cloud.zookeeper.enabled=true` 参数

   ```bash
   java -jar alpha-server-${saga_version}-exec.jar \ 
     --spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false \
     --spring.datasource.username=saga \
     --spring.datasource.password=saga \
     --spring.cloud.zookeeper.enabled=true \
     --spring.cloud.zookeeper.connectString=${zookeeper_host}:${zookeeper_port} \
     --spring.profiles.active=prd 
   ```

   **注意:** `${zookeeper_host}` 是 zookeeper 地址, `${zookeeper_port}` 是 zookeeper 端口

   **注意:** 更多 Zookeeper 参数请参考 [Spring Cloud Zookeeper 2.x](https://cloud.spring.io/spring-cloud-zookeeper/spring-cloud-zookeeper.html) [Spring Cloud Zookeeper 1.x](https://cloud.spring.io/spring-cloud-static/spring-cloud-zookeeper/1.2.2.RELEASE/single/spring-cloud-zookeeper.html)

2. 验证是否注册成功

   访问Zookeeper的实例, 在znode /services/servicecomb-alapha-server 下，查看服务注册znode, 在注册的znode中，存在类似以下值

    ```json
    {
        "name": "servicecomb-alpha-server",
        "id": "9b2223ae-50e6-49a6-9f3b-87a1ff06a016",
        "address": "arch-office",
        "port": 8090,
        "sslPort": null,
        "payload": {
            "@class": "org.springframework.cloud.zookeeper.discovery.ZookeeperInstance",
            "id": "servicecomb-alpha-server-1",
            "name": "servicecomb-alpha-server",
            "metadata": {
            "servicecomb-alpha-server": "arch-office:8080"
            }
        },
        "registrationTimeUTC": 1558000134185,
        "serviceType": "DYNAMIC",
        "uriSpec": {
            "parts": [
            {
                "value": "scheme",
                "variable": true
            },
            {
                "value": "://",
                "variable": false
            },
            {
                "value": "address",
                "variable": true
            },
            {
                "value": ":",
                "variable": false
            },
            {
                "value": "port",
                "variable": true
            }
            ]
        }
    }
    ```
   **注意:** 默认情况下注册的服务名是`servicecomb-alpha-server`,如果你需要自定义服务名可以在运行Alpha的时候通过命令行参数`spring.application.name`配置

3. 配置omega

   在项目中引入依赖包 `omega-spring-cloud-zookeeper-starter`

   ```xml
   <dependency>
   	<groupId>org.apache.servicecomb.pack</groupId>
   	<artifactId>omega-spring-cloud-zookeeper-starter</artifactId>
   	<version>${pack.version}</version>
   </dependency>
   ```

   在 `application.yaml` 添加下面的配置项：

   ```yaml
   spring:
     cloud:
       zookeeper:
         enabled: true
         connectString: 127.0.0.1:2181
         
   alpha:
     cluster:
       register:
         type: zookeeper
   ```

   - `spring.cloud.zookeeper.connectString` 配置 Zookeeper 注册中心的地址，更多zookeeper客户端配置可以参考[Spring Cloud Zookeeper 2.x](https://cloud.spring.io/spring-cloud-zookeeper/spring-cloud-zookeeper.html) [Spring Cloud Zookeeper 1.x](https://cloud.spring.io/spring-cloud-static/spring-cloud-zookeeper/1.2.2.RELEASE/single/spring-cloud-zookeeper.html)


   - `alpha.cluster.register.type=zookeeper` 配置Omega获取Alpha的方式是通过 Zookeeper 的注册中心

   - spring boot 版本兼容

     如果你的项目使用的不是 spring boot 2.3.X 版本，那么请参照此列表增加兼容的spring-cloud-starter-zookeeper-discovery版本

     | spring boot    | spring-cloud-starter-zookeeper-discovery |
     |------------------------------------------| -------------------------------------    |
     | 2.3.12.RELEASE | 2.2.5.RELEASE                            | 
     | 2.1.x.RELEASE  | 2.1.1.RELEASE                            |
     | 1.5.17.RELEASE | 1.2.2.RELEASE                            |

     ```xml
       <dependencyManagement>
         <dependencies>
           <dependency>
             <groupId>org.springframework.cloud</groupId>
             <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
             <version>2.2.5.RELEASE</version>
           </dependency>
         </dependencies>
       </dependencyManagement>
     ```

   **注意:** 如果你在启动Alpha的时候通过命令行参数`spring.application.name`自定义了服务名，那么你需要在Omega中通过参数`alpha.cluster.serviceId`指定这个服务名

### Spring Cloud Nacos Discovery 支持

1. 运行alpha

   运行时增加 `nacos.client.enabled=true` 参数

   ```bash
   java -jar alpha-server-${saga_version}-exec.jar \ 
     --spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false \
     --spring.datasource.username=saga \
     --spring.datasource.password=saga \
     --spring.cloud.nacos.discovery.enabled=true \
     --spring.cloud.nacos.discovery.serverAddr=${nacos_host}:${nacos_port} \
     --nacos.client.enabled=true \
     --spring.profiles.active=prd 
   ```

   **注意:** `${nacos_host}` 是 nacos 地址, `${nacos_port}` 是 nacos 端口

   **注意:** 更多 Nacos 参数请参考 [Spring Cloud Nacos Discovery ](https://nacos.io/zh-cn/docs/quick-start-spring-cloud.html)
   
2. 验证是否注册成功

   访问Nacos的实例, 通过nacos 提供的openapi`curl -X GET 'http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=servicecomb-alpha-server‘` 可以看到如下服务注册信息，在metadata 中可以发现gRPC的地址已经被注册


   ```json
    {
    "metadata": {},
    "dom": "servicecomb-alpha-server",
    "cacheMillis": 3000,
    "useSpecifiedURL": false,
    "hosts": [
        {
        "valid": true,
        "marked": false,
        "metadata": {
            "preserved.register.source": "SPRING_CLOUD",
            "servicecomb-alpha-server": "192.168.2.28:8080"
        },
        "instanceId": "192.168.2.28#8090#DEFAULT#DEFAULT_GROUP@@servicecomb-alpha-server",
        "port": 8090,
        "healthy": true,
        "ip": "192.168.2.28",
        "clusterName": "DEFAULT",
        "weight": 1,
        "ephemeral": true,
        "serviceName": "servicecomb-alpha-server",
        "enabled": true
        }
    ],
    "name": "DEFAULT_GROUP@@servicecomb-alpha-server",
    "checksum": "d9e8deefd1c4f198980f4443d7c1b1fd",
    "lastRefTime": 1562567653565,
    "env": "",
    "clusters": ""
    }
   ```
   **注意:** 默认情况下注册的服务名是`servicecomb-alpha-server`,如果你需要自定义服务名可以在运行Alpha的时候通过命令行参数`spring.application.name`配置

3. 配置omega

   在项目中引入依赖包 `omega-spring-cloud-nacos-starter`

   ```xml
   <dependency>
   	<groupId>org.apache.servicecomb.pack</groupId>
   	<artifactId>omega-spring-cloud-nacos-starter</artifactId>
   	<version>${pack.version}</version>
   </dependency>
   ```

   在 `application.yaml` 添加下面的配置项：

   ```yaml
   spring:
     cloud:
       nacos:
         discovery:
           enabled: true
           serverAddr: 127.0.0.1:8848
         
   alpha:
     cluster:
       register:
         type: nacos
   ```

   - `spring.cloud.nacos.discovery.serverAddr` 配置 Nacos 注册中心的地址，更多Nacos 参数请参考 [Spring Cloud Nacos Discovery ](https://nacos.io/zh-cn/docs/quick-start-spring-cloud.html)


   - `alpha.cluster.register.type=nacos` 配置Omega获取Alpha的方式是通过 Nacos 的注册中心

   - spring boot 版本兼容

     如果你的项目使用的不是 spring boot 2.3.X 版本，那么请参照此列表增加兼容的spring-cloud-starter-alibaba-nacos-discovery版本

     | spring boot    | spring-cloud-starter-alibaba-nacos-discovery |
----------------| -------------  | -------------------------------------    |
     | 2.3.12.RELEASE  | 2.2.6.RELEASE                            | 
     | 2.1.x.RELEASE  | 0.2.2.RELEASE                            |
     | 1.5.17.RELEASE | 0.1.2.RELEASE                            |

     ```xml
       <dependencyManagement>
         <dependencies>
           <dependency>
             <groupId>com.alibaba.cloud</groupId>
             <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
             <version>2.2.6.RELEASE</version>
           </dependency>
         </dependencies>
       </dependencyManagement>
     ```

   **注意:** 如果你在启动Alpha的时候通过命令行参数`spring.application.name`自定义了服务名，那么你需要在Omega中通过参数`alpha.cluster.serviceId`指定这个服务名

## 集群

Alpha 可以通过部署多实例的方式保证高可用，使用 `alpha.cluster.master.enabled=true` 参数开启集群支持

## Native transports

Alpha 使用 `alpha.feature.nativetransport=true` 开启JNI调用Socket Transport的功能，与基于NIO的传输相比，这些JNI传输添加了特定于特定平台的功能，产生了更少的垃圾，并总体上提高性能

[src-TransactionClientHttpRequestInterceptor]: ../omega/omega-transport/omega-transport-resttemplate/src/main/java/org/apache/servicecomb/pack/omega/transport/resttemplate/TransactionClientHttpRequestInterceptor.java
[src-TransactionHandlerInterceptor]: ../omega/omega-transport/omega-transport-resttemplate/src/main/java/org/apache/servicecomb/pack/omega/transport/resttemplate/TransactionHandlerInterceptor.java

## Pack 分布式事务规则

[Saga-Akka 分布式状态机规格](spec-saga-akka/fsm_manual_zh.md)

## 升级指南

[Pack 0.6.0 升级指南](./upgrade_guide/0.6.0-upgrade_guide_zh.md)

