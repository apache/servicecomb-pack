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
1. run the following command to create docker images in saga project root folder.
   ```
   mvn clean install -DskipTests -Pdocker -Pdemo
   ```
2. start the whole application up(including alpha server and three demo services)
   ```
   ./saga-dubbo-demo.sh up
   ```
    **Note:** If you prefer to use MySQL as alpha's backend database, you need to try the following steps instead:
    1. add dependency of `mysql-connector-java` in `alpha/alpha-server/pom.xml`
     ```xml
         <dependency>
           <groupId>mysql</groupId>
           <artifactId>mysql-connector-java</artifactId>
         </dependency>
     ```
    2. re-generate saga's docker images in saga project root folder
     ```bash
     mvn package -DskipTests -Pdocker -Pdemo
     ```
    3. start application up in `saga-dubbo-demo` with the following command
     ```
	 cd ./saga-demo/saga-dubbo-demo
     ./saga-dubbo-demo.sh up-mysql
     ```

    **Note:** If you want start alpha server and demon services separately, you can try the following steps:
    1. start alpha server
     ```bash
         ./saga-dubbo-demo.sh up-alpha
     ```
    2. when alpha server started complatelly, then start the demo services
     ```bash
         ./saga-dubbo-demo.sh up-demo
     ```
4. stop application
   ```
   ./saga-dubbo-demo.sh down
   ```

Use browser to run transaction demos:
A:servicea B:serviceb C:servicec

A->B
```
 http://${host_address}:8071/serviceInvoke/Ab
```

A->B (A throw an exception)
```
 http://${host_address}:8071/serviceInvoke/AExceptionWhenAb
```

A->B (B throw an exception)
```
 http://${host_address}:8071/serviceInvoke/BExceptionWhenAb
```

A->B A->C
```
 http://${host_address}:8071/serviceInvoke/AbAc
```

A->B A->C (C throw an exception)
```
 http://${host_address}:8071/serviceInvoke/CExceptionWhenAbAc
```

A->B B->C
```
 http://${host_address}:8071/serviceInvoke/AbBc
```

A->B B->C (C throw an exception)
```
 http://${host_address}:8071/serviceInvoke/CExceptionWhenAbBc
```

## Debugging
Take the [spring-demo debugging](../saga-spring-demo#debugging) as a reference.
