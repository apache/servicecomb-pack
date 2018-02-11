**Problem:** How to use MySQL as alpha's backend database?

**Solution:** To replace the default database from postgreSQL to MySQL:
1. add dependency of `mysql-connector-java` in `alpha/alpha-server/pom.xml`
   ```xml
       <dependency>
         <groupId>mysql</groupId>
         <artifactId>mysql-connector-java</artifactId>
       </dependency>
   ```
   
2. activate mysql profile by specifying option `-Dspring.profiles.active=mysql` when booting alpha.