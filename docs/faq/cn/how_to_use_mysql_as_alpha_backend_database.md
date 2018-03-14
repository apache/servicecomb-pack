**问题描述:** 如何使用MySQL作为alpha的后台数据库？

**解决方法:** 使用MySQL来替换默认的数据库PostgreSQL，只需以下两步：
1. 在`alpha/alpha-server/pom.xml`添加`mysql-connector-java`的依赖：
   ```xml
       <dependency>
         <groupId>mysql</groupId>
         <artifactId>mysql-connector-java</artifactId>
       </dependency>
   ```

2. 安装Saga
   ```bash
   mvn clean package -Pdocker -DskipTests
   ```
   在命令执行完成后，会生成名为alpha-server的镜像和可执行文件`alpha/alpha-server/target/saga/alpha-server-${version}-exec.jar`。

   **注意**: 如果不需要生成docker镜像，则直接运行`mvn clean package -DskipTests`即可。

   **注意**: 如果您之前已生成了alpha-server的docker镜像，则需要先执行以下命令将其删除：
   ```bash
   docker rmi -f $(docker images | grep alpha-server | awk '{print $3}')
   ```
   
3. 运行MySQL
   ```bash
   docker run -d -e "MYSQL_ROOT_PASSWORD=password" -e "MYSQL_DATABASE=saga" -e "MYSQL_USER=saga" -e "MYSQL_PASSWORD=password" -p 3306:3306 mysql/mysql-server:5.7
   ```

4. 运行alpha。请确保MySQL在此前已成功启动。alpha的运行可通过docker或可执行文件的方式。
   * 通过docker
      ```bash
      docker run -d -p 8080:8080 -p 8090:8090 -e "JAVA_OPTS=-Dspring.profiles.active=mysql -Dspring.datasource.url=jdbc:mysql://${host_address}:3306/saga?useSSL=false" alpha-server:${saga_version}
      ```
   * 通过可执行文件
      ```bash
      java -Dspring.profiles.active=mysql -D"spring.datasource.url=jdbc:mysql://${host_address}:3306/saga?useSSL=false" -jar alpha-server-${saga_version}-exec.jar
      ```
   **注意**: 请在运行命令前将`${saga_version}`和`${host_address}`更改为实际值。

   **注意**: 默认情况下，8080端口用于处理omega处发起的gRPC的请求，而8090端口用于处理查询存储在alpha处的事件信息。

