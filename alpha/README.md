# alpha-server

The alpha-server works as the pack leader to keep the consistency of transactions.
For more information, see [saga pack design](https://github.com/apache/incubator-servicecomb-saga/blob/master/docs/design.md)

## Build and Run

### Via docker image
Build the executable files and docker image:
```
mvn clean package -DskipTests -Pdocker -Pdemo
```

Then play `alpha-server` image with docker, docker-compose or any other container based environment.

You can override the configurations by `JAVA_OPTS` environment variable:
```
docker run -d -p 8080:8080 -p 8090:8090 \ 
-e "JAVA_OPTS=-Dspring.profiles.active=prd -Dspring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" alpha-server:${saga_version}
```


### Via executable file

Build the executable files:
```bash
mvn clean package -DskipTests -Pdemo
```

And run:
```
java -Dspring.profiles.active=prd -D"spring.datasource.url=jdbc:postgresql://${host_address}:5432/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar
```
