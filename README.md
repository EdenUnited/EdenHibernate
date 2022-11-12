# EdenHibernate
This library aims to support hibernate in Minecraft-Plugins while keeping the jars small,
it works by loading the needed dependencies at runtime.

```yml
format: "mysql"
#format: "mariadb"
#format: "postgresql"
path: "jdbc:mysql://localhost/database"
#path: "jdbc:postgresql://localhost/database"
#path: "jdbc:mariadb://localhost/database"
user: "root"
password: "1234"
```
```yml
format: "sqlite"
#format: "h2"
path: "jdbc:sqlite:./plugins/my_plugin/data.db"
#path: "jdbc:h2:./data.db"
```
