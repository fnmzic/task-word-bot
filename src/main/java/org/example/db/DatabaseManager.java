package org.example.db;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/your_database_name"); // Укажите URL вашей базы данных
        config.setUsername(System.getenv("DATABASE_USERNAME")); // Укажите имя пользователя
        config.setPassword(System.getenv("DATABASE_PASSWORD")); // Укажите пароль
        config.setMaximumPoolSize(10); // Максимальное количество соединений в пуле
        config.setMinimumIdle(2); // Минимальное количество соединений в пуле
        config.setIdleTimeout(30000); // Время простоя соединения перед закрытием (в миллисекундах)
        config.setMaxLifetime(1800000); // Максимальное время жизни соединения (в миллисекундах)

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}