**问题描述:** 如何使用MySQL作为alpha的后台数据库？

**解决方法:** 使用MySQL来替换默认的数据库PostgreSQL，只需以下两步：
1. 运行MySQL
   ```bash
   docker run -d -e "MYSQL_ROOT_PASSWORD=password" -e "MYSQL_DATABASE=saga" -e "MYSQL_USER=saga" -e "MYSQL_PASSWORD=password" -p 3306:3306 mysql/mysql-server:5.7
   ```

2. 下载MySQL jdbc jar

   ```bash
   wget http://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-8.0.15.tar.gz 
   tar -xvzf mysql-connector-java-8.0.15.tar.gz
   ```

3. 运行alpha。请确保MySQL在此前已成功启动。alpha的运行可通过docker或可执行文件的方式。

   * 通过docker

      在本机器创建 `plugins` 目录，用来挂载镜像内部目录 `/maven/saga/plugins` ，复制  `mysql-connector-java-8.0.15.jar` 到 `plugins` 目录，使用  `-Dloader.path=/maven/saga/plugins`  参数添加这个目录下所有的 JAR 到 classpath

      ```bash
      docker run -d -p 8080:8080 -p 8090:8090 -v ./plugins:/maven/saga/plugins -e "JAVA_OPTS=-Dspring.profiles.active=mysql -Dloader.path=/maven/saga/plugins -Dspring.datasource.url=jdbc:mysql://${host_address}:3306/saga?serverTimezone=GMT%2b8&useSSL=false" alpha-server:${saga_version}
      ```

   * 通过可执行文件

      在 alpha-server-${saga_version}-exec.jar 同级目录下创建 `plugins` 目录 ，复制  `mysql-connector-java-8.0.15.jar` 到 `plugins` 目录中，使用  `-Dloader.path=/maven/saga/plugins`  参数添加这个目录下所有的 JAR 到 classpath

      ```bash
      java -Dspring.profiles.active=mysql -Dloader.path=./plugins -D"spring.datasource.url=jdbc:mysql://${host_address}:3306/saga?serverTimezone=GMT%2b8&useSSL=false" -jar alpha-server-${saga_version}-exec.jar
      ```
      **注意**: 请在运行命令前将`${saga_version}`和`${host_address}`更改为实际值。

   **注意**: 默认情况下，8080端口用于处理omega处发起的gRPC的请求，而8090端口用于处理查询存储在alpha处的事件信息。

   **注意**: 请确保MySQL连接串中设置了正确的时区参数`serverTimezone=GMT%2b8`。

