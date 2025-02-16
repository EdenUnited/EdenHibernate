# EdenHibernate
This library aims to support hibernate in Minecraft-Plugins while keeping the jars small,
it works by loading the needed dependencies at runtime.

### Remote Databases
```yml
format: "mysql"
#format: "mariadb"
#format: "postgresql"
path: "127.0.0.1/database"
user: "root"
password: "1234"
```
### File Databases
```yml
format: "sqlite"
#format: "h2"
path: "data.db"
```
