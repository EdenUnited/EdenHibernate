package at.haha007.edenhibernate;

enum DatabaseType {
    SQLITE("org.sqlite.JDBC", "jdbc:sqlite:./plugins/%plugin%/", "org.hibernate.community.dialect.SQLiteDialect"),
    H2("org.h2.Driver", "jdbc:h2:./plugins/%plugin%/", "org.hibernate.dialect.H2Dialect"),
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql:", "org.hibernate.dialect.MySQLDialect"),
    MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb:", "org.hibernate.dialect.MariaDBDialect"),
    POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql:", "org.hibernate.dialect.PostgreSQLDialect");

    private final String driverClass;
    private final String pathPrefix;
    private final String dialect;

    DatabaseType(String driverClass, String pathPrefix, String dialect) {
        this.driverClass = driverClass;
        this.pathPrefix = pathPrefix;
        this.dialect = dialect;
    }

    public String driverClass() {
        return driverClass;
    }

    public String pathPrefix() {
        return pathPrefix;
    }

    public String dialect() {
        return dialect;
    }

    public static DatabaseType fromString(String in) {
        if (in == null) throw new IllegalArgumentException();
        in = in.toLowerCase();
        return switch (in) {
            case "sqlite" -> SQLITE;
            case "h2" -> H2;
            case "mysql" -> MYSQL;
            case "mariadb" -> MARIADB;
            case "postgresql" -> POSTGRESQL;
            default -> throw new IllegalArgumentException();
        };
    }
}
