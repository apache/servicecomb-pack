# Booking Demo
This demo simulates a booking application including three services:
* booking
* car
* hotel

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

   **Note:** If you want start alpha server and demon services separatelly, you can try the following steps:
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
   1. start hotel service. The executable jar file should be in `saga-demo/booking/hotel/target/saga`.
   ```bash
   java -Dserver.port=8081 -Dalpha.cluster.address=${alpha_address}:8080 -jar hotel-${saga_version}-exec.jar
   ```

   2. start car service. The executable jar file should be in `saga-demo/booking/car/target/saga`.
   ```bash
   java -Dserver.port=8082 -Dalpha.cluster.address=${alpha_address}:8080 -jar car-${saga_version}-exec.jar
   ```

   3. start booking service. The executable jar file should be in `saga-demo/booking/booking/target/saga`.
   ```bash
   java -Dserver.port=8083 -Dalpha.cluster.address=${alpha_address}:8080 -Dcar.service.address=${host_address}:8082 -Dhotel.service.address=${host_address}:8081  -jar booking-${saga_version}-exec.jar
   ```

## User Requests by command line tools
1. Booking 2 rooms and 2 cars, this booking will be OK.
```
curl -X POST http://${host_address}:8083/booking/test/2/2
```
Check the hotel booking status with
```
curl http://${host_address}:8081/bookings
```
Check the car booking status with
```
curl http://${host_address}:8082/bookings

```

2. Booking 3 rooms and 2 cars, this booking will case the hotel order failed and trigger the compansate operation with car booking.
```
curl -X POST http://${host_address}:8083/booking/test/3/2
```
Check the hotel booking status with
```
curl http://${host_address}:8081/bookings
```
Check the car booking status with
```
curl http://${host_address}:8082/bookings
```
The second car booking will be marked with **cancel:true**

## User Requests by html page

Open a browser with URL http://127.0.0.1:8083, You will get a html page. You can use this page to invoke test cases, and then get results.

**Note** transactions and compensations implemented by services must be idempotent.
