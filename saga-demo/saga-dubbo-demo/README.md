# Saga-dubbo-demo
This demo including three services:
* servicea
* serviceb
* servicec

## Prerequisites
You will need:
1. [JDK 1.8][jdk]
2. [Maven 3.x][maven]
3. [alpha server][alpha_server]

[jdk]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[maven]: https://maven.apache.org/install.html
[alpha_server]: https://github.com/apache/incubator-servicecomb-saga/tree/master/alpha

## Running Demo
you can run saga-dubbo-demo as normal spring application
1.init database: sql/schema-mysql.sql, and config database info by enviroment params
enviroment params example:
-Ddatabase.url=jdbc:mysql://127.0.0.1:3306/saga_dubbo
-Ddatabase.username=root
-Ddatabase.password=***
-Dzookeeper.url=localhost:2181
-Dserver.port=8071

2.config zookeeper url by enviroment param
3.run servicea, serviceb, servicec,visit http://${servicea_host}:${servicea_port}