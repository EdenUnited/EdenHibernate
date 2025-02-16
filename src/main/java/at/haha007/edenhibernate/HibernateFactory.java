package at.haha007.edenhibernate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HibernateFactory {

    private final boolean showSql;

    public HibernateFactory(boolean showSql) {
        this.showSql = showSql;
    }

    public HibernateFactory() {
        this(false);
    }

    public static ConfigurationSection createDefaultConfig() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> types = Arrays.stream(DatabaseType.values())
                .map(DatabaseType::name)
                .map(String::toLowerCase)
                .toList();
        cfg.set("type", DatabaseType.MYSQL.name().toLowerCase());
        cfg.setComments("type", List.of("Possible types: " + types));
        cfg.set("path", "127.0.0.1:3306/database");
        cfg.setInlineComments("username", List.of("Ignored by Sqlite and H2"));
        cfg.set("username", "root");
        cfg.setInlineComments("password", List.of("Ignored by Sqlite and H2"));
        cfg.set("password", "admin");
        return cfg;
    }

    public SessionFactory fromYaml(ConfigurationSection cfg, boolean debug, Collection<Class<?>> annotatedClasses) {
        DatabaseType type = DatabaseType.fromString(cfg.getString("type"));
        String path = cfg.getString("path");
        if (path == null) throw new NullPointerException("Path not configured!");
        String username = cfg.getString("username");
        if (username == null) throw new NullPointerException("Username not configured!");
        String password = cfg.getString("password");
        if (password == null) throw new NullPointerException("Password not configured!");
        return switch (type) {
            case MYSQL -> createMySql(path, username, password, annotatedClasses);
            case POSTGRESQL -> createPostgres(path, username, password, annotatedClasses);
            case SQLITE -> createSqlite(new File(path), annotatedClasses);
            case H2 -> createH2(new File(path), annotatedClasses);
            case MARIADB -> createMariaDB(path, username, password, annotatedClasses);
        };
    }

    public SessionFactory createPostgres(@NotNull String path,
                                         @NotNull String username,
                                         @NotNull String password,
                                         @NotNull Collection<Class<?>> annotatedClasses) {
        return createPostgres(path, username, password, Map.of(), annotatedClasses);
    }

    public SessionFactory createPostgres(@NotNull String path,
                                         @NotNull String username,
                                         @NotNull String password,
                                         @NotNull Map<String, String> customConfigurationValues,
                                         @NotNull Collection<Class<?>> annotatedClasses) {
        Configuration configuration = createRemote(DatabaseType.POSTGRESQL, path, username, password);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    public SessionFactory createMySql(@NotNull String path,
                                      @NotNull String username,
                                      @NotNull String password,
                                      @NotNull Collection<Class<?>> annotatedClasses) {
        return createMySql(path, username, password, Map.of(), annotatedClasses);
    }

    public SessionFactory createMySql(@NotNull String path,
                                      @NotNull String username,
                                      @NotNull String password,
                                      @NotNull Map<String, String> customConfigurationValues,
                                      @NotNull Collection<Class<?>> annotatedClasses) {
        Configuration configuration = createRemote(DatabaseType.MYSQL, path, username, password);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }


    public SessionFactory createMariaDB(@NotNull String path,
                                        @NotNull String username,
                                        @NotNull String password,
                                        @NotNull Collection<Class<?>> annotatedClasses) {
        return createMariaDB(path, username, password, Map.of(), annotatedClasses);
    }

    public SessionFactory createMariaDB(@NotNull String path,
                                        @NotNull String username,
                                        @NotNull String password,
                                        @NotNull Map<String, String> customConfigurationValues,
                                        @NotNull Collection<Class<?>> annotatedClasses) {
        Configuration configuration = createRemote(DatabaseType.MARIADB, path, username, password);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    public SessionFactory createH2(File file, Collection<Class<?>> annotatedClasses) {
        return createH2(file, Map.of(), annotatedClasses);
    }

    public SessionFactory createH2(@NotNull File file,
                                   @NotNull Map<String, String> customConfigurationValues,
                                   @NotNull Collection<Class<?>> annotatedClasses) {
        Configuration configuration = createLocal(DatabaseType.H2, file);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    public SessionFactory createSqlite(@NotNull File file, @NotNull Collection<Class<?>> annotatedClasses) {
        return createSqlite(file, Map.of(), annotatedClasses);
    }

    public SessionFactory createSqlite(@NotNull File file,
                                       @NotNull Map<String, String> customConfigurationValues,
                                       @NotNull Collection<Class<?>> annotatedClasses) {
        Configuration configuration = createLocal(DatabaseType.SQLITE, file);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    private Configuration createLocal(DatabaseType type, File file) {
        Configuration configuration = new Configuration();

        //set driver and provider
        hibernateInit(configuration, type.driverClass(), type.dialect());

        //url and login
        configuration.setProperty("hibernate.connection.url", type.pathPrefix() + file.getAbsolutePath());

        //debuging?
        showSqlInit(configuration);

        //hikariCP properties, use customConfigurationValues to change them
        hikariInit(configuration, 5, 20);
        return configuration;
    }


    private Configuration createRemote(DatabaseType type, String url, String user, String password) {
        Configuration configuration = new Configuration();

        //set driver and provider
        hibernateInit(configuration, type.driverClass(), type.dialect());

        //url and login
        configuration.setProperty("hibernate.connection.url", type.pathPrefix() + url);
        configuration.setProperty("hibernate.connection.username", user);
        configuration.setProperty("hibernate.connection.password", password);

        //debuging?
        showSqlInit(configuration);

        //hikariCP properties, use customConfigurationValues to change them
        hikariInit(configuration, 20, 300);
        return configuration;
    }


    private void hibernateInit(Configuration configuration, String driver, String dialect) {
        configuration.setProperty("hibernate.current_session_context_class", "thread");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");
        configuration.setProperty("hibernate.connection.driver_class", driver);
        configuration.setProperty("hibernate.dialect", dialect);
    }

    private void hikariInit(Configuration configuration, int minIdle, int maxPoolSize) {
        configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        configuration.setProperty("hibernate.hikari.connectionTimeout", "10000");
        configuration.setProperty("hibernate.hikari.minimumIdle", String.valueOf(minIdle));
        configuration.setProperty("hibernate.hikari.maximumPoolSize", String.valueOf(maxPoolSize));
        configuration.setProperty("hibernate.hikari.idleTimeout", "120000");
    }

    private void showSqlInit(Configuration configuration) {
        configuration.setProperty("hibernate.show_sql", String.valueOf(showSql));
        configuration.setProperty("hibernate.format_sql", String.valueOf(showSql));
        configuration.setProperty("hibernate.use_sql_comments", String.valueOf(showSql));
    }
}
