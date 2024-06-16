package at.haha007.edenhibernate;

import org.jetbrains.annotations.NotNull;

public abstract class DatabaseDialect {
    @NotNull
    private final String driverClassPath;

    protected DatabaseDialect(@NotNull String driverClassPath) {
        this.driverClassPath = driverClassPath;
    }

    public String getDriverClassPath() {
        return driverClassPath;
    }
}
