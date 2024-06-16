package at.haha007.edenhibernate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HibernateFactory {

    @NotNull
    private final JavaPlugin plugin;

    public HibernateFactory(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static ConfigurationSection createDefaultConfig() {
        YamlConfiguration cfg = new YamlConfiguration();
        List<String> types = Arrays.stream(DatabaseType.values())
                .map(DatabaseType::name)
                .map(String::toLowerCase)
                .toList();
        cfg.set("type", DatabaseType.MYSQL.name().toLowerCase());
        cfg.setComments("type", List.of("Possible types: " + types));
        cfg.set("url", "localhost:3306/database");
        cfg.setInlineComments("username", List.of("Ignored by Sqlite and H2"));
        cfg.set("username", "root");
        cfg.setInlineComments("password", List.of("Ignored by Sqlite and H2"));
        cfg.set("password", "admin");
        return cfg;
    }

    public SessionFactory fromYaml(ConfigurationSection cfg, boolean debug, Collection<Class<?>> annotatedClasses) {
        DatabaseType type = DatabaseType.fromString(cfg.getString("type"));
        String url = cfg.getString("url");
        if (url == null) throw new NullPointerException("URL not configured!");
        String username = cfg.getString("username");
        if (username == null) throw new NullPointerException("Username not configured!");
        String password = cfg.getString("password");
        if (password == null) throw new NullPointerException("Password not configured!");
        return switch (type) {
            case MYSQL -> createMySql(url, username, password, annotatedClasses);
            case POSTGRESQL -> createPostgres(url, username, password, annotatedClasses);
            case SQLITE -> createSqlite(new File(url), annotatedClasses);
            case H2 -> createH2(new File(url), annotatedClasses);
            case MARIADB -> createMariaDB(url, username, password, annotatedClasses);
        };
    }

    public SessionFactory createPostgres(@NotNull String url,
                                         @NotNull String username,
                                         @NotNull String password,
                                         @NotNull Collection<Class<?>> annotatedClasses) {
        return createPostgres(url, username, password, Map.of(), annotatedClasses, false);
    }

    public SessionFactory createPostgres(@NotNull String url,
                                         @NotNull String username,
                                         @NotNull String password,
                                         @NotNull Map<String, String> customConfigurationValues,
                                         @NotNull Collection<Class<?>> annotatedClasses,
                                         boolean showSql) {
        Configuration configuration = createRemote(DatabaseType.POSTGRESQL, url, username, password, showSql);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    public SessionFactory createMySql(@NotNull String url,
                                      @NotNull String username,
                                      @NotNull String password,
                                      @NotNull Collection<Class<?>> annotatedClasses) {
        return createMySql(url, username, password, Map.of(), annotatedClasses, false);
    }

    public SessionFactory createMySql(@NotNull String url,
                                      @NotNull String username,
                                      @NotNull String password,
                                      @NotNull Map<String, String> customConfigurationValues,
                                      @NotNull Collection<Class<?>> annotatedClasses,
                                      boolean showSql) {
        Configuration configuration = createRemote(DatabaseType.MYSQL, url, username, password, showSql);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }


    public SessionFactory createMariaDB(@NotNull String url,
                                        @NotNull String username,
                                        @NotNull String password,
                                        @NotNull Collection<Class<?>> annotatedClasses) {
        return createMariaDB(url, username, password, Map.of(), annotatedClasses, false);
    }

    public SessionFactory createMariaDB(@NotNull String url,
                                        @NotNull String username,
                                        @NotNull String password,
                                        @NotNull Map<String, String> customConfigurationValues,
                                        @NotNull Collection<Class<?>> annotatedClasses,
                                        boolean showSql) {
        Configuration configuration = createRemote(DatabaseType.MARIADB, url, username, password, showSql);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    public SessionFactory createH2(File file, Collection<Class<?>> annotatedClasses) {
        return createH2(file, Map.of(), annotatedClasses, false);
    }

    public SessionFactory createH2(@NotNull File file,
                                   @NotNull Map<String, String> customConfigurationValues,
                                   @NotNull Collection<Class<?>> annotatedClasses,
                                   boolean showSql) {
        Configuration configuration = createLocal(DatabaseType.H2, file, showSql);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    public SessionFactory createSqlite(@NotNull File file, @NotNull Collection<Class<?>> annotatedClasses) {
        return createSqlite(file, Map.of(), annotatedClasses, false);
    }

    public SessionFactory createSqlite(@NotNull File file,
                                       @NotNull Map<String, String> customConfigurationValues,
                                       @NotNull Collection<Class<?>> annotatedClasses,
                                       boolean showSql) {
        Configuration configuration = createLocal(DatabaseType.SQLITE, file, showSql);
        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }

    private Configuration createLocal(DatabaseType type, File file, boolean showSql) {
        Configuration configuration = new Configuration();

        //set driver and provider
        hibernateInit(configuration, type.driverClass(), type.dialect());

        //url and login
        configuration.setProperty("hibernate.connection.url", type.pathPrefix() + file.getPath());

        //debuging?
        showSqlInit(configuration, showSql);

        //hikariCP properties, use customConfigurationValues to change them
        hikariInit(configuration, 5, 20);
        return configuration;
    }


    private Configuration createRemote(DatabaseType type, String url, String user, String password, boolean showSql) {
        Configuration configuration = new Configuration();

        //set driver and provider
        hibernateInit(configuration, type.driverClass(), type.dialect());

        //url and login
        configuration.setProperty("hibernate.connection.url", type.pathPrefix() + url);
        configuration.setProperty("hibernate.connection.username", user);
        configuration.setProperty("hibernate.connection.password", password);

        //debuging?
        showSqlInit(configuration, showSql);

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

    private void showSqlInit(Configuration configuration, boolean showSql) {
        configuration.setProperty("hibernate.show_sql", String.valueOf(showSql));
        configuration.setProperty("hibernate.format_sql", String.valueOf(showSql));
        configuration.setProperty("hibernate.use_sql_comments", String.valueOf(showSql));
    }
}
