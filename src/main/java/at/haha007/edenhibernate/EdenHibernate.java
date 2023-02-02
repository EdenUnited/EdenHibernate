package at.haha007.edenhibernate;

import at.haha007.mavenloader.MavenLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public final class EdenHibernate implements AutoCloseable {
    private final SessionFactory factory;
    private final JavaPlugin plugin;
    private MavenLoader mavenLoader;


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
        mavenLoader.close();
    }

    private SessionFactory load(
            ConfigurationSection config,
            Map<String, String> customConfigurationValues,
            boolean showSql,
            List<Class<?>> annotatedClasses) {
        DatabaseType type = DatabaseType.fromString(config.getString("format"));
        loadDependencies(type);
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
        configuration.setProperty("hibernate.show_sql", "" + showSql);
        configuration.setProperty("hibernate.format_sql", "" + showSql);
        configuration.setProperty("hibernate.use_sql_comments", "" + showSql);

        //hikariCP properties, use customConfigurationValues to change them
        configuration.setProperty("hibernate.hikari.connectionTimeout", "10000");
        configuration.setProperty("hibernate.hikari.minimumIdle", "20");
        configuration.setProperty("hibernate.hikari.maximumPoolSize", "300");
        configuration.setProperty("hibernate.hikari.idleTimeout", "200000");


        customConfigurationValues.forEach(configuration::setProperty);

        annotatedClasses.forEach(configuration::addAnnotatedClass);


        return configuration.buildSessionFactory();

    }

    private void loadDependencies(DatabaseType type) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(generateDependencyJson("org.hibernate.orm", "hibernate-core", "6.1.6.Final"));
        jsonArray.add(generateDependencyJson("org.hibernate.common", "hibernate-commons-annotations", "6.0.2.Final"));
        jsonArray.add(generateDependencyJson("org.hibernate.orm", "hibernate-hikaricp", "6.1.6.Final"));
        jsonArray.add(generateDependencyJson("com.zaxxer", "HikariCP", "5.0.1"));
        jsonArray.add(generateDependencyJson("jakarta.persistence", "jakarta.persistence-api", "3.1.0"));
        jsonArray.add(generateDependencyJson("jakarta.transaction", "jakarta.transaction-api", "2.0.0"));
        jsonArray.add(generateDependencyJson("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.1"));
        jsonArray.add(generateDependencyJson("jakarta.activation", "jakarta.activation-api", "2.1.0"));
        jsonArray.add(generateDependencyJson("org.jboss.logging", "jboss-logging", "3.4.3.Final"));
        jsonArray.add(generateDependencyJson("com.fasterxml", "classmate", "1.5.1"));
        jsonArray.add(generateDependencyJson("net.bytebuddy", "byte-buddy", "1.12.18"));
        jsonArray.add(generateDependencyJson("org.glassfish.jaxb", "jaxb-runtime", "3.0.2"));
        jsonArray.add(generateDependencyJson("org.glassfish.jaxb", "jaxb-core", "3.0.2"));
        jsonArray.add(generateDependencyJson("org.antlr", "antlr4-runtime", "4.10.1"));
        jsonArray.add(generateDependencyJson("com.sun.istack", "istack-commons-runtime", "4.1.1"));
        switch (type) {
            case SQLITE -> {
                jsonArray.add(generateDependencyJson("org.hibernate.orm", "hibernate-community-dialects", "6.1.6.Final"));
                jsonArray.add(generateDependencyJson("org.xerial", "sqlite-jdbc", "3.39.3.0"));
            }
            case H2 -> {
                jsonArray.add(generateDependencyJson("com.h2database", "h2", "2.1.214"));
            }
            case MYSQL -> {
                jsonArray.add(generateDependencyJson("mysql", "mysql-connector-java", "8.0.31"));
            }
            case MARIADB -> {
                jsonArray.add(generateDependencyJson("org.mariadb.jdbc", "mariadb-java-client", "3.0.8"));
            }
            case POSTGRESQL -> {
                jsonArray.add(generateDependencyJson("org.postgresql", "postgresql", "42.5.0"));
            }
            default -> throw new RuntimeException();
        }
        JsonObject dependencyConfig = new JsonObject();
        dependencyConfig.add("dependencies", jsonArray);
        try {
            Constructor<MavenLoader> constructor = MavenLoader.class.getDeclaredConstructor(JavaPlugin.class, String.class);
            constructor.setAccessible(true);
            this.mavenLoader = constructor.newInstance(plugin, dependencyConfig.toString());
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject generateDependencyJson(String group, String artifact, String version) {
        JsonObject element = new JsonObject();
        element.addProperty("group", group);
        element.addProperty("artifact", artifact);
        element.addProperty("version", version);
        return element;
    }

}
