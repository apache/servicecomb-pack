**问题描述:** 如何使用MySQL作为alpha的后台数据库？

**解决方法:** 使用MySQL来替换默认的数据库PostgreSQL，只需以下两步：
1. 在`alpha/alpha-server/pom.xml`添加`mysql-connector-java`的依赖：
   ```xml
       <dependency>
         <groupId>mysql</groupId>
         <artifactId>mysql-connector-java</artifactId>
       </dependency>
   ```
   
2. 在alpha启动时通过添加`-Dspring.profiles.active=mysql`的启动参数来使mysql的配置生效。
