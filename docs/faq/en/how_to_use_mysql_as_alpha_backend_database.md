**Problem:** How to use MySQL as alpha's backend database?

**Solution:** To replace the default database from postgreSQL to MySQL:
1. run MySQL
   ```bash
   docker run -d -e "MYSQL_ROOT_PASSWORD=password" -e "MYSQL_DATABASE=saga" -e "MYSQL_USER=saga" -e "MYSQL_PASSWORD=password" -p 3306:3306 mysql/mysql-server:5.7
   ```

2. Run alpha. Please make sure MySQL is up before this step. You can run alpha through docker or executable file.
   * via docker

      Create `libs`  directory on the machine, for mount containers internal directory `/maven/saga/libs` , copy ` mysql-connector-java-8.0.15.jar` to the libs directory,  uses the `-Dloader.path=/maven/saga/libs` system property and includes all JAR files in this directory and add them in the classpath 

      ```bash
      docker run -d -p 8080:8080 -p 8090:8090 -v ./libs:/maven/saga/libs -e "JAVA_OPTS=-Dspring.profiles.active=mysql -Dloader.path=/maven/saga/libs -Dspring.datasource.url=jdbc:mysql://${host_address}:3306/saga?useSSL=false" alpha-server:${saga_version}
      ```

   * via executable file

      Create `libs`  directory in the same directory as alpha-server-${saga_version}-exec.jar, copy `mysql-connector-java-8.0.15.jar` to the `libs` directory, uses the `-Dloader.path=/maven/saga/libs` system property and includes all JAR files in this directory and add them in the classpath 

      ```bash
      java -Dspring.profiles.active=mysql -Dloader.path=./libs -D"spring.datasource.url=jdbc:mysql://${host_address}:3306/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar
      ```

   **Notice**: Please change `${saga_version}` and `${host_address}` to the actual value before you execute the command.


   **Note**: By default, port 8080 is used to serve omega's request via gRPC while port 8090 is used to query the events stored in alpha.


   **Note**: Please configuration serverTimezone property in MySQL connection string
