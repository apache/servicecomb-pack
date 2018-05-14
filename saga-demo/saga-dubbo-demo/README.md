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
1.mvn clean install -DskipTests -Pdocker
2. ./saga-dubbo-demo.sh up

