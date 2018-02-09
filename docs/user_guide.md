# Saga 用户指南
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
To build:
```bash
$ git clone https://github.com/apache/incubator-servicecomb-saga.git
$ cd incubator-servicecomb-saga
$ mvn clean install -DskipTests -Pdocker
```

## How to use
### Add saga dependencies
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
   **Note:** By default, timeout is disable.

   **Note:** If the starting point of global transaction and local transaction overlaps, both `@SagaStart` and `@Compensable` are needed.

4. Repeat step 3 for the `transferIn` service.

## How to run
1. run postgreSQL.
   ```bash
   docker run -d -e "POSTGRES_DB=saga" -e "POSTGRES_USER=saga" -e "POSTGRES_PASSWORD=password" -p 5432:5432 postgres
   ```

2. run alpha. Before running alpha, please make sure postgreSQL is already up.
   ```bash
   docker run -d -p 8090:8090 \
     -e "JAVA_OPTS=-Dspring.profiles.active=prd" \
     -e "spring.datasource.url=jdbc:postgresql://{docker.host.address}:5432/saga?useSSL=false" \
     alpha-server:0.0.3-SNAPSHOT
   ```

3. setup omega. Configure the following values in `application.yaml`.
   ```yaml
   spring:
     application:
       name: {application.name}
   alpha:
     cluster:
       address: {alpha.cluster.addresses}
   ```

Then you can start your micro-services.
