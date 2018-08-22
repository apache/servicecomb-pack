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
```bash
$ mvn clean install -DskipTests -Pdocker
$ ./saga-dubbo-demo.sh up
```

## Debugging
Take the [spring-demo debugging](../saga-spring-demo#debugging) as a reference.
