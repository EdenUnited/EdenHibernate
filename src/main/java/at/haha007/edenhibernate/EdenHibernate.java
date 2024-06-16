package at.haha007.edenhibernate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;
import java.util.Map;

@Deprecated
public final class EdenHibernate implements AutoCloseable {
    private final SessionFactory factory;
    private final JavaPlugin plugin;


    public EdenHibernate(JavaPlugin plugin, List<Class<?>> annotatedClasses) {
        this(plugin, annotatedClasses, false);
    }

    public EdenHibernate(JavaPlugin plugin, List<Class<?>> annotatedClasses, boolean showSql) {
        this(plugin, annotatedClasses, showSql, Map.of(), plugin.getConfig());
    }

    public EdenHibernate(JavaPlugin plugin,
                         List<Class<?>> annotatedClasses,
                         boolean showSql,
                         Map<String, String> customConfigValues,
                         ConfigurationSection config) {
        this.plugin = plugin;
        factory = load(config, customConfigValues, showSql, annotatedClasses);
    }

    public SessionFactory factory() {
        return factory;
    }

    public void close() {
        factory.close();
    }

    private SessionFactory load(
            ConfigurationSection config,
            Map<String, String> customConfigurationValues,
            boolean showSql,
            List<Class<?>> annotatedClasses) {
        DatabaseType type = DatabaseType.fromString(config.getString("format"));
        Configuration configuration = new Configuration();

        //set driver and provider
        configuration.setProperty("hibernate.connection.driver_class", type.driverClass());
        configuration.setProperty("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");

        //url and login
        configuration.setProperty("hibernate.connection.url", type.pathPrefix().replace("%plugin%", plugin.getName()) + config.getString("path"));
        configuration.setProperty("hibernate.connection.username", config.getString("user"));
        configuration.setProperty("hibernate.connection.password", config.getString("password"));

        //hibernate properties
        configuration.setProperty("hibernate.dialect", type.dialect());
        configuration.setProperty("hibernate.current_session_context_class", "thread");
        configuration.setProperty("hibernate.hbm2ddl.auto", "update");

        //debuging?
        configuration.setProperty("hibernate.show_sql", String.valueOf(showSql));
        configuration.setProperty("hibernate.format_sql", String.valueOf(showSql));
        configuration.setProperty("hibernate.use_sql_comments", String.valueOf(showSql));

        //hikariCP properties, use customConfigurationValues to change them
        configuration.setProperty("hibernate.hikari.connectionTimeout", "10000");
        configuration.setProperty("hibernate.hikari.minimumIdle", "20");
        configuration.setProperty("hibernate.hikari.maximumPoolSize", "300");
        configuration.setProperty("hibernate.hikari.idleTimeout", "200000");


        customConfigurationValues.forEach(configuration::setProperty);
        annotatedClasses.forEach(configuration::addAnnotatedClass);
        return configuration.buildSessionFactory();
    }
}
