# Pack User Guide
[![ZH doc](https://img.shields.io/badge/document-中文-blue.svg)](user_guide_zh.md)

## Prerequisites
You will need:
1. [JDK 1.8][jdk]
2. [Maven 3.x][maven]
3. [Docker][docker]

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[maven]: https://maven.apache.org/install.html
[docker]: https://www.docker.com/get-docker

## Build

Retrieve the source code:
```bash
$ git clone https://github.com/apache/servicecomb-pack.git
$ cd servicecomb-pack
```

Saga can be built in either of the following ways.
* Only build the executable files.
   ```bash
   $ mvn clean install -DskipTests
   ```

* build the executable files along with docker image.
   ```bash
   $ mvn clean install -DskipTests -Pdocker
   ```
   
* build the executable file and saga-distribution
   ```bash
      $ mvn clean install -DskipTests -Prelease
   ```

After executing either one of the above command, you will find alpha server's executable file in `alpha/alpha-server/target/saga/alpha-server-${version}-exec.jar`.

## How to use
### Add pack dependencies
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
**Note**: Please change the `${pack.version}` to the actual version. 

**Migration Note**:Since 0.3.0 we rename the project repository name from saga to pack. Please update the group id and package name if you migrate your application from saga 0.2.x to pack 0.3.0. 

|  name    |  0.2.x     |  0.3.x    |
| ---- | ---- | ---- |
|  groupId    | org.apache.servicecomb.saga     |  org.apache.servicecomb.pack   |
| Package Name | org.apache.servicecomb.saga     |  org.apache.servicecomb.pack   |

### Saga Support
Add saga annotations and corresponding compensation methods
Take a transfer money application as an example:
1. Add `@SagaStart` at the starting point of the global transaction to prepare the new global transaction context. If you don't specify the SagaStart the flowing sub-transaction will complain that the global transaction id is not found.
   ```java
   import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;
   
   @SagaStart(timeout=10)
   public boolean transferMoney(String from, String to, int amount) {
     transferOut(from, amount);
     transferIn(to, amount);
   }
   ```
   **Note:** By default, timeout is disable.

2. Add `@Compensable` at the sub-transaction and specify its corresponding compensation method.
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

   **Note** The transactions and compensations method should have same arguments. The transactions and compensations implemented by services must be idempotent. We highly recommend to use the Spring @Transactional to guarantee the local transaction.
                                                                                                                                                                                                                                                               

   **Note:** By default, timeout is disable.

   **Note:** If the starting point of global transaction and local transaction overlaps, both `@SagaStart` and `@Compensable` are needed.

3. Add alpha.cluster.address parameters

   ```yaml
   alpha:
     cluster:
       address: alpha-server.servicecomb.io:8080
   ```
4. Add omega.spec.names parameters

   ```yaml
   omega:
     spec:
       names: saga
   ```
   
5. Repeat step 2 for the `transferIn` service.

6. Since pack-0.3.0,  you can access the [OmegaContext](https://github.com/apache/servicecomb-pack/blob/master/omega/omega-context/src/main/java/org/apache/servicecomb/pack/omega/context/OmegaContext.java) for the gloableTxId and localTxId in the @Compensable annotated method or the cancel method.

7. Sinc pack-0.7.0, You can change the distributed transaction specification through the alpha.spec.names parameter, currently supported modes are saga-db (default), tcc-db, saga-akka
#### <a name="explicit-tx-context-passing"></a>Passing transaction context explicitly

In most cases, Omega passing the transaction context for you transparently (see [Inter-Service Communication](design.md#comm) for details). Transaction context passing is implemented in a way of injecting transaction context information on the sender side and extracting it on the receiver side. Below is an example to illustrate this process:

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

Here is how Omega does:

1. Service A's `foo` method opens a new global transaction.
2. [TransactionClientHttpRequestInterceptor][src-TransactionClientHttpRequestInterceptor] injects transaction context into request headers when `RestTemplate` request Service B.
3. When Service B receive the request, [TransactionHandlerInterceptor][src-TransactionHandlerInterceptor] extract context info from request headers.

Omega supports following implicit transaction context passing:

1. omega-transport-{dubbo,feign,resttemplate,servicecomb}. Please make sure you add these transport artifacts into your classpath, otherwise you may face an issue that Omega complains about cannot find global transaction id.
2. Method call in the same thread (based on `OmegaContext` thread local fields).
3. `java.util.concurrent.Executor{Service}` annotated by `@OmegaContextAware`.

So here comes a problem: what if implicit transaction context passing can't work? For example, Service A invokes Service B via some RPC library and no extension can be made to injecting or extracting transaction context information. In this situation you need explicit transaction context passing. Since ServiceComb Pack 0.5.0, it provides two classes to achieve that.

##### TransactionContext

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

Notice that `bar` method got an injected transaction context in parameter list, and got a local transaction context from `OmegaContext`. If service B needs to explictly pass transaction context to another service, local transaction context should be used.

##### TransactionContextProperties

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

Similar to the previous approach, `cmdWithTxContext.get{Global,Local}TxId()` also returns injected transaction context information.

#### End a Saga manually

Since pack-0.5.0 an attribute name `autoClose` is added to `@SagaStart` annotation, this attribute is used to control whether a SagaEndedEvent should be sent to Alpha after `SagaStart` annotated method is executed (default value is `true`). When `autoClose=false` you should use `@SagaEnd` to send SagaEndedEvent manually, for example:

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

### TCC support

Add TCC annotations and corresponding confirm and cancel methods
 Take a transfer money application as an example:
 1. add `@TccStart` at the starting point of the global transaction
    ```java
    import org.apache.servicecomb.pack.omega.context.annotations.TccStart;
    
    @TccStart
    public boolean transferMoney(String from, String to, int amount) {
      transferOut(from, amount);
      transferIn(to, amount);
    }
    ```
    **Note:** By default, timeout is disable.

 2. add `@Participate` at the sub-transaction and specify its corresponding compensation method
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
                                                                                              
    **Note:** Current TCC implementation doesn't support timeout.

    **Note:** If the starting point of global transaction and local transaction overlaps, both `@TccStart` and `@Participate` are needed.

 3. Add alpha.cluster.address parameters

    ```yaml
    alpha:
      cluster:
        address: alpha-server.servicecomb.io:8080
    ```
 4. Add omega.spec.names parameters

   ```yaml
   omega:
     spec:
       names: tcc
   ```
   
 5. Repeat step 2 for the `transferIn` service.

#### Passing transaction context explicitly

Just like Saga's `@Compensable`，`@Participate` also supports explicit transaction passing. Please refer to [Saga - Passing transaction context explicitly](#explicit-tx-context-passing) for more details.


## How to run
1. run postgreSQL.
   ```bash
   docker run -d -e "POSTGRES_DB=saga" -e "POSTGRES_USER=saga" -e "POSTGRES_PASSWORD=password" -p 5432:5432 postgres
   ```
   Please check out [this document](https://github.com/apache/servicecomb-pack/blob/master/docs/faq/en/how_to_use_mysql_as_alpha_backend_database.md), if you want to use the MySQL instead of postgreSQL.

2. run alpha. Before running alpha, please make sure postgreSQL is already up. You can run alpha through docker or executable file.
   * Run alpha through docker.
      ```bash
      docker run -d -p 8080:8080 -p 8090:8090 -e "JAVA_OPTS=-Dspring.profiles.active=prd -Dspring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" alpha-server:${saga_version}
      ```
   * Run alpha through executable file.
      ```bash
      java -Dspring.profiles.active=prd -D"spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar
      ```

   **Note**: Please change `${pack_version}` and `${host_address}` to the actual value before you execute the command.

   **Note**: By default, port 8080 is used to serve omega's request via gRPC while port 8090 is used to query the events stored in alpha.

3. setup omega. Configure the following values in `application.yaml`.
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

Then you can start your micro-services and access all saga events via http://${alpha-server:port}/saga/events.

## Enable SSL for Alpha and Omega

See [Enabling SSL](enable_ssl.md) for details.

## Service discovery support

Alpha instance can register to the discovery service, Omega obtains Alpha's instance list and gRPC address through discovery service

### Consul

1. run alpha

   run with parameter `spring.cloud.consul.enabled=true`

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

   **Note:** `${consul_host}` is consul host, `${consul_port}` is consul port

   **Note:** Check out  for more details  [Spring Cloud Consul 2.x](https://cloud.spring.io/spring-cloud-consul/spring-cloud-consul.html) [Spring Cloud Consul 1.x](https://cloud.spring.io/spring-cloud-consul/1.3.x/single/spring-cloud-consul.html)

2. verify registration information

   request `curl http://127.0.0.1:8500/v1/agent/services`, It responds with the following JSON

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

   **Note:**  `Tags` property is alpha gRPC address

   **Note:** alpha instance name is `servicecomb-alpha-server` by default. You can set it by starting parameter  `spring.application.name` 

3. setup omega

   edit your `pom.xml` and add the `omega-spring-cloud-consul-starter` dependency

   ```xml
   <dependency>
    <groupId>org.apache.servicecomb.pack</groupId>
    <artifactId>omega-spring-cloud-consul-starter</artifactId>
    <version>${pack.version}</version>
   </dependency>
   ```

   edit your  `application.yaml` , as shown in the following example:

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

   - `spring.cloud.consul.host` property is set to the Consul server’s instance address, `spring.cloud.consul.port` property is set to the Consul server’s instance port, `spring.cloud.consul.discovery.register=false` property is not register yourself , check out Spring Boot’s  [Spring Cloud Consul 2.x](https://cloud.spring.io/spring-cloud-consul/spring-cloud-consul.html)  or [Spring Cloud Consul 1.x](https://cloud.spring.io/spring-cloud-consul/1.3.x/single/spring-cloud-consul.html) for more details.

   - `alpha.cluster.register.type=consul`  property is omega gets alpha gRPC address from Consul

   - spring boot version compatible

     If your project is not using spring boot 2.3.X, please refer to this list to add a compatible spring-cloud-starter-consul-discovery version

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

     

   **Note:** If you define `spring.application.name ` parameter when start alpha,  You need to specify this service name in Omega via the parameter `alpha.cluster.serviceId`

### Spring Cloud Eureka

1. build version of eureka

   build the version support eureka with the  `-Pspring-cloud-eureka`  parameter

   ```bash
   git clone https://github.com/apache/servicecomb-pack.git
   cd servicecomb-pack
   mvn clean install -DskipTests=true -Pspring-cloud-eureka
   ```

2. run alpha

   run with parameter `eureka.client.enabled=true`

   ```bash
   java -jar alpha-server-${saga_version}-exec.jar \ 
     --spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false \
     --spring.datasource.username=saga \
     --spring.datasource.password=saga \
     --eureka.client.enabled=true \
     --eureka.client.service-url.defaultZone=http://127.0.0.1:8761/eureka \  
     --spring.profiles.active=prd 
   ```
   **Note:** Check out  [Spring Cloud Netflix 2.x](https://cloud.spring.io/spring-cloud-netflix/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter) [Spring Cloud Netflix 1.x](https://cloud.spring.io/spring-cloud-netflix/1.4.x/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter) for more details

3. verify registration information

   request `curl http://127.0.0.1:8761/eureka/apps/`, It responds with the following XML

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

   **Note:**  `<servicecomb-alpha-server>` property is alpha gRPC address

   **Note:** alpha instance name is `SERVICECOMB-ALPHA-SERVER` by default. You can set it by starting parameter  `spring.application.name` 

4. setup omega

   edit your `pom.xml` and add the `omega-spring-cloud-eureka-starter` dependency

   ```xml
   <dependency>
    <groupId>org.apache.servicecomb.pack</groupId>
    <artifactId>omega-spring-cloud-eureka-starter</artifactId>
    <version>${pack.version}</version>
   </dependency>
   ```

   edit your  `application.yaml` , as shown in the following example:

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

   * `eureka.client.service-url.defaultZone` property is set to the Eureka server’s instance address, check out Spring Boot’s  [Spring Cloud Netflix 2.x](https://cloud.spring.io/spring-cloud-netflix/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter) or [Spring Cloud Netflix 1.x](https://cloud.spring.io/spring-cloud-netflix/1.4.x/multi/multi__service_discovery_eureka_clients.html#netflix-eureka-client-starter) for more details.

   * `alpha.cluster.register.type=eureka`  property is omega gets alpha gRPC address from Eureka

   * spring boot version compatible

     If your project is not using spring boot 2.3.X, please refer to this list to add a compatible spring-cloud-starter-netflix-eureka-client version

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

   **Note:** If you define `spring.application.name ` parameter when start alpha,  You need to specify this service name in Omega via the parameter `alpha.cluster.serviceId`

### Spring Cloud Zookeeper

1. run alpha

   run with parameter `spring.cloud.zookeeper.enabled=true`

   ```bash
   java -jar alpha-server-${saga_version}-exec.jar \ 
     --spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false \
     --spring.datasource.username=saga \
     --spring.datasource.password=saga \
     --spring.cloud.zookeeper.enabled=true \
     --spring.cloud.zookeeper.connectString=${zookeeper_host}:${zookeeper_port} \
     --spring.profiles.active=prd 
   ```

   **Note:** `${zookeeper_host}` is zookeeper host, `${zookeeper_port}` is zookeeper port

   **Note:** Check out  for more details  [Spring Cloud Zookeeper 2.x](https://cloud.spring.io/spring-cloud-zookeeper/spring-cloud-zookeeper.html) [Spring Cloud Zookeeper 1.x](https://cloud.spring.io/spring-cloud-static/spring-cloud-zookeeper/1.2.2.RELEASE/single/spring-cloud-zookeeper.html)


2. verify registration information

   view znode /services/servicecomb-alapha-server 

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

   **Note:**  `metadata` property is alpha gRPC address

   **Note:** alpha instance name is `servicecomb-alpha-server` by default. You can set it by starting parameter  `spring.application.name` 

3. setup omega

   edit your `pom.xml` and add the `omega-spring-cloud-zookeeper-starter` dependency

   ```xml
   <dependency>
    <groupId>org.apache.servicecomb.pack</groupId>
    <artifactId>omega-spring-cloud-zookeeper-starter</artifactId>
    <version>${pack.version}</version>
   </dependency>
   ```

   edit your  `application.yaml` , as shown in the following example:

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

   - `spring.cloud.zookeeper.connectString` property is set to the Zookeeper server’s instance address, check out Spring Boot’s  [Spring Cloud Zookeeper 2.x](https://cloud.spring.io/spring-cloud-zookeeper/spring-cloud-zookeeper.html) [Spring Cloud Zookeeper 1.x](https://cloud.spring.io/spring-cloud-static/spring-cloud-zookeeper/1.2.2.RELEASE/single/spring-cloud-zookeeper.html) for more details.

   - `alpha.cluster.register.type=zookeeper`  property is omega gets alpha gRPC address from Zookeeper

   - spring boot version compatible

     If your project is not using spring boot 2.3.X, please refer to this list to add a compatible spring-cloud-starter-zookeeper-discovery version

     | spring boot    | spring-cloud-starter-zookeeper-discovery |
     |------------------------------------------| ------------------------------------- |
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

     

   **Note:** If you define `spring.application.name ` parameter when start alpha,  You need to specify this service name in Omega via the parameter `alpha.cluster.serviceId`


### Spring Cloud Nacos Discovery

1. run alpha

   run with parameter `nacos.client.enabled=true`

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

   **Note:** `${nacos_host}` is nacos host, `${nacos_port}` is nacos port

   **Note:** Check out  for more details  [Spring Cloud Nacos Discovery ](https://nacos.io/en-us/docs/quick-start-spring-cloud.html)


2. verify registration information

   request `curl -X GET 'http://127.0.0.1:8848/nacos/v1/ns/instance/list?serviceName=servicecomb-alpha-server‘` , It responds with the following JSON

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

   **Note:**  `metadata` property is alpha gRPC address

   **Note:** alpha instance name is `servicecomb-alpha-server` by default. You can set it by starting parameter  `spring.application.name` 

3. setup omega

   edit your `pom.xml` and add the `omega-spring-cloud-nacos-starter` dependency

   ```xml
   <dependency>
    <groupId>org.apache.servicecomb.pack</groupId>
    <artifactId>omega-spring-cloud-nacos-starter</artifactId>
    <version>${pack.version}</version>
   </dependency>
   ```

   edit your  `application.yaml` , as shown in the following example:

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

   - `spring.cloud.nacos.discovery.serverAddr` property is set to the Nacos server’s instance address, check out Spring Boot’s  [Spring Cloud Nacos Discovery ](https://nacos.io/en-us/docs/quick-start-spring-cloud.html) for more details.

   - `alpha.cluster.register.type=nacos`  property is omega gets alpha gRPC address from Nacos

   - spring boot version compatible

     If your project is not using spring boot 2.3.X, please refer to this list to add a compatible spring-cloud-starter-alibaba-nacos-discovery version

     | spring boot    | spring-cloud-starter-alibaba-nacos-discovery |
----------------| -------------  | ------------------------------------- |
     | 2.3.12.RELEASE  | 2.2.6.RELEASE                         |
     | 2.1.x.RELEASE  | 0.2.2.RELEASE                         |
     | 1.5.17.RELEASE | 0.1.2.RELEASE                         |

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

     

   **Note:** If you define `spring.application.name ` parameter when start alpha,  You need to specify this service name in Omega via the parameter `alpha.cluster.serviceId`


## Cluster

Alpha can be highly available by deploying multiple instances, enable cluster support with the `alpha.cluster.master.enabled=true` parameter.

[src-TransactionClientHttpRequestInterceptor]: ../omega/omega-transport/omega-transport-resttemplate/src/main/java/org/apache/servicecomb/pack/omega/transport/resttemplate/TransactionClientHttpRequestInterceptor.java
[src-TransactionHandlerInterceptor]: ../omega/omega-transport/omega-transport-resttemplate/src/main/java/org/apache/servicecomb/pack/omega/transport/resttemplate/TransactionHandlerInterceptor.java

## Native transports

Alpha enabled JNI transports support with `alpha.feature.nativetransport=true`, These JNI transports add features specific to a particular platform, generate less garbage, and generally improve performance when compared to the NIO based transport.

## Pack Distributed Transaction Specifications

[Saga-Akka Specifications](spec-saga-akka/fsm_manual.md)

## Upgrade Guide

[Pack 0.6.0 Migration Guide](./upgrade_guide/0.6.0-upgrade_guide.md)