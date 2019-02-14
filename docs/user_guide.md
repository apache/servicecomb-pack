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
1. add `@EnableOmega` at application entry to initialize omega configurations and connect to alpha
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
   
2. add `@SagaStart` at the starting point of the global transaction
   ```java
   import org.apache.servicecomb.pack.omega.context.annotations.SagaStart;

   @SagaStart(timeout=10)
   public boolean transferMoney(String from, String to, int amount) {
     transferOut(from, amount);
     transferIn(to, amount);
   }
   ```
   **Note:** By default, timeout is disable.

3. add `@Compensable` at the sub-transaction and specify its corresponding compensation method
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

4. Repeat step 3 for the `transferIn` service.

5. Since pack-0.3.0,  you can access the [OmegaContext](https://github.com/apache/servicecomb-packblob/master/omega/omega-context/src/main/java/org/apache/servicecomb/saga/omega/context/OmegaContext.java) for the gloableTxId and localTxId in the @Compensable annotated method or the cancel method.

### TCC support
Add TCC annotations and corresponding confirm and cancel methods
 Take a transfer money application as an example:
 1. add `@EnableOmega` at application entry to initialize omega configurations and connect to alpha
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
    
 2. add `@TccStart` at the starting point of the global transaction
    ```java
    import org.apache.servicecomb.pack.omega.context.annotations.TccStart;
    
    @TccStart
    public boolean transferMoney(String from, String to, int amount) {
      transferOut(from, amount);
      transferIn(to, amount);
    }
    ```
    **Note:** By default, timeout is disable.
 
 3. add `@Participate` at the sub-transaction and specify its corresponding compensation method
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
 
 4. Repeat step 3 for the `transferIn` service.


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
   ```

Then you can start your micro-services and access all saga events via http://${alpha-server:port}/saga/events.

## Enable SSL for Alpha and Omega

See [Enabling SSL](enable_ssl.md) for details.
