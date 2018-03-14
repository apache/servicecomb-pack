**Problem:** How to use MySQL as alpha's backend database?

**Solution:** To replace the default database from postgreSQL to MySQL:
1. add dependency of `mysql-connector-java` in `alpha/alpha-server/pom.xml`
   ```xml
       <dependency>
         <groupId>mysql</groupId>
         <artifactId>mysql-connector-java</artifactId>
       </dependency>
   ```

2. install Saga
   ```bash
   mvn clean package -Pdocker -DskipTests
   ```
   After that, you will find the generated docker image `alpha-server` and executable file `alpha/alpha-server/target/saga/alpha-server-${version}-exec.jar`.

   **Notice**: If you do not want to build the docker image, run `mvn clean package -DskipTests` is enough.

   **Notice**: If you have installed saga with docker before, you need to remove the alpha-server's docker image first.
   ```bash
   docker rmi -f $(docker images | grep alpha-server | awk '{print $3}')
   ```

3. run MySQL
   ```bash
   docker run -d -e "MYSQL_ROOT_PASSWORD=password" -e "MYSQL_DATABASE=saga" -e "MYSQL_USER=saga" -e "MYSQL_PASSWORD=password" -p 3306:3306 mysql/mysql-server:5.7
   ```

4. Run alpha. Please make sure MySQL is up before this step. You can run alpha through docker or executable file.
   * via docker
      ```bash
      docker run -d -p 8080:8080 -p 8090:8090 -e "JAVA_OPTS=-Dspring.profiles.active=mysql -Dspring.datasource.url=jdbc:mysql://${host_address}:3306/saga?useSSL=false" alpha-server:${saga_version}
      ```
   * via executable file
      ```bash
      java -Dspring.profiles.active=mysql -D"spring.datasource.url=jdbc:mysql://${host_address}:3306/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar
      ```

   **Notice**: Please change `${saga_version}` and `${host_address}` to the actual value before you execute the command.


   **Note**: By default, port 8080 is used to serve omega's request via gRPC while port 8090 is used to query the events stored in alpha.
