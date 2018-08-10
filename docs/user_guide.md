# Saga User Guide
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
$ git clone https://github.com/apache/incubator-servicecomb-saga.git
$ cd incubator-servicecomb-saga
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
### Add saga dependencies
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
**Note**: Please change the `${saga.version}` to the actual version.

### Add saga annotations and corresponding compensation methods
Take a transfer money application as an example:
1. add `@EnableOmega` at application entry to initialize omega configurations and connect to alpha
   ```java
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
   @SagaStart(timeout=10)
   public boolean transferMoney(String from, String to, int amount) {
     transferOut(from, amount);
     transferIn(to, amount);
   }
   ```
   **Note:** By default, timeout is disable.

3. add `@Compensable` at the sub-transaction and specify its corresponding compensation method
   ```java
   @Compensable(timeout=5, compensationMethod="cancel")
   public boolean transferOut(String from, int amount) {
     repo.reduceBalanceByUsername(from, amount);
   }
 
   public boolean cancel(String from, int amount) {
     repo.addBalanceByUsername(from, amount);
   }
   ```

   **Note** transactions and compensations implemented by services must be idempotent.

   **Note:** By default, timeout is disable.

   **Note:** If the starting point of global transaction and local transaction overlaps, both `@SagaStart` and `@Compensable` are needed.

4. Repeat step 3 for the `transferIn` service.

5. Since Saga-0.3.0,  you can access the [OmegaContext](https://github.com/apache/incubator-servicecomb-saga/blob/master/omega/omega-context/src/main/java/org/apache/servicecomb/saga/omega/context/OmegaContext.java) for the gloableTxId and localTxId in the @Compensable annotated method or the cancel method.

## How to run
1. run postgreSQL.
   ```bash
   docker run -d -e "POSTGRES_DB=saga" -e "POSTGRES_USER=saga" -e "POSTGRES_PASSWORD=password" -p 5432:5432 postgres
   ```
   Please check out [this document](https://github.com/apache/incubator-servicecomb-saga/blob/master/docs/faq/en/how_to_use_mysql_as_alpha_backend_database.md), if you want to use the MySQL instead of postgreSQL.

2. run alpha. Before running alpha, please make sure postgreSQL is already up. You can run alpha through docker or executable file.
   * Run alpha through docker.
      ```bash
      docker run -d -p 8080:8080 -p 8090:8090 -e "JAVA_OPTS=-Dspring.profiles.active=prd -Dspring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" alpha-server:${saga_version}
      ```
   * Run alpha through executable file.
      ```bash
      java -Dspring.profiles.active=prd -D"spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar
      ```

   **Note**: Please change `${saga_version}` and `${host_address}` to the actual value before you execute the command.

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

Then you can start your micro-services and access all saga events via http://${alpha-server:port}/events.

## Enable SSL for Alpha and Omega

See [Enabling SSL](enable_ssl.md) for details.
