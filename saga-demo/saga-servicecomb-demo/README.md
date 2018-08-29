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
2. Enter the saga servicecomb demo directory and give permissions to script
   ```
   cd ./saga-demo/saga-servicecomb-demo
   chmod +x saga-servicecomb-demo.sh
   ```
3. start the whole application up(including alpha server and three demo services)
    ```
    ./saga-servicecomb-demo.sh up
    ```
4. stop application
   ```
   ./saga-servicecomb-demo.sh down
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

2. Booking 3 rooms and 2 cars, this booking will cause the hotel order failed and trigger the compensate operation with car booking.
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

## Debugging
Take the [spring-demo debugging](../saga-spring-demo#debugging) as a reference.
