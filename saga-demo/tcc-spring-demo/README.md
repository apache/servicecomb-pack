# Booking Demo
This demo simulates a ordering application including three services:
* ordering
* inventory
* payment

## Prerequisites
You will need:
1. [JDK 1.8][jdk]
2. [Maven 3.x][maven]
3. [Docker][docker]
4. [Docker compose][docker_compose]
5. [alpha server][alpha_server]

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[maven]: https://maven.apache.org/install.html
[docker]: https://www.docker.com/get-docker
[docker_compose]: https://docs.docker.com/compose/install/
[alpha_server]: https://github.com/apache/incubator-servicecomb-saga/tree/master/alpha

## Running Demo
You can run the demo using either docker compose or executable files.
### via docker compose
1. run the following command to create docker images in saga project root folder.
   ```
   mvn clean package -DskipTests -Pdocker -Pdemo
   ```

2. start the whole application up(including alpha server and three demo services)
   ```
   ./saga-demo.sh up
   ```

   **Note:** If you prefer to use MySQL as alpha's backend database, you need to try the following steps instead:
   1. add dependency of `mysql-connector-java` in `alpha/alpha-server/pom.xml`
      ```xml
          <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
          </dependency>
      ```
   2. remove alpha server's docker image
      ```bash
      docker rmi -f $(docker images | grep alpha-server | awk '{print $3}')
      ```
   3. re-generate saga's docker images
      ```bash
      mvn package -DskipTests -Pdocker -Pdemo
      ```
   4. start application up in `saga-demo/booking` with the following command
      ```
      ./saga-demo.sh up-mysql
      ```

   **Note:** If you want start alpha server and demon services separately, you can try the following steps:
   1. start alpha server
      ```bash
          ./saga-demo.sh up-alpha
      ```
   2. when alpha server started complatelly, then start the demo services
      ```bash
          ./saga-demo.sh up-demo
      ```

3. stop application
   ```
   ./saga-demo.sh down
   ```

### via executable files
1. run the following command to generate executable alpha server jar in `alpha/alpha-server/target/saga/alpha-server-${saga_version}-exec.jar`.
   ```
   mvn clean package -DskipTests -Pdemo
   ```

2. follow the instructions in the [How to run](https://github.com/apache/incubator-servicecomb-saga/blob/master/docs/user_guide.md#how-to-run) section in User Guide to run postgreSQL and alpha server.

3. start application up
   1. start inventory service. The executable jar file should be in `saga-demo/tcc-spring-demo/inventory/target/saga`.
   ```bash
   java -Dserver.port=8081 -Dalpha.cluster.address=${alpha_address}:8080 -jar tcc-inventory-${saga_version}-exec.jar
   ```

   2. start payment service. The executable jar file should be in `saga-demo/tcc-spring-demo/payment/target/saga`.
   ```bash
   java -Dserver.port=8082 -Dalpha.cluster.address=${alpha_address}:8080 -jar tcc-payment-${saga_version}-exec.jar
   ```

   3. start ordering service. The executable jar file should be in `saga-demo/tcc-spring-demo/ordering/target/saga`.
   ```bash
   java -Dserver.port=8083 -Dalpha.cluster.address=${alpha_address}:8080 -Dinventory.service.address=${host_address}:8082 -Dpayment.service.address=${host_address}:8081  -jar tcc-ordering-${saga_version}-exec.jar
   ```

## User Requests by command line tools
1. Ordering 2 units ProductA with the unit price 1 from UserA account, this ordering will be OK.
```
curl -X POST http://${host_address}:8083/ordering/order/UserA/ProductA/3/1
```
Check the Inventory orders status with
```
curl http://${host_address}:8082/orderings
```
Check the Payment transaction status with
```
curl http://${host_address}:8081/transactions

```

2. Since the initial value of the payment of UserC is 1. Ordering 2 units of ProductA with the unit price 2 from UserC account , this ordering will cause the payment failed and trigger the cancel operation with inventory ordering.
```
curl -X POST http://${host_address}:8083/ordering/order/UserC/ProductA/2/2
```
Check the Inventory orders status with
```
curl http://${host_address}:8082/orderings
```
Check the Payment transaction status with
```
curl http://${host_address}:8081/transactions
```
The second order will be marked with **cancel:true**

## Debugging

To debug the services of the demo, just add debug parameter to JVM through the environment field in docker-compose configs. Let's take alpha-server as an example:

```yaml
alpha:
  image: "alpha-server:${TAG}"
  environment:
    - JAVA_OPTS=-Dspring.profiles.active=prd -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
  ports:
    - "6006:5005"
...
```

We append the debug parameter `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005` to the JAVA_OPTS environment variable, the Java process inside the container will listen on port 5005. With the port forwarding rule `6006:5005`, when alpha-server is ready, we can connect to port 6006 on the host and start debugging alpha-server.

If  you're using [IntelliJ](https://www.jetbrains.com/idea/), open the saga project, create a new debug configuration with template 'Remote', fill "Host" and "Port" input with "localhost" and "6006", then select "alpha" in the drop-down list of "Use classpath of module". When alpha-server is running, hit shift+f9 to debug the remote application.
