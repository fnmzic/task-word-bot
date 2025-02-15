package org.example.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        String sql = "CREATE TABLE IF NOT EXISTS \"user\" (" +
                "id BIGINT PRIMARY KEY, " +
                "isPolicyAccepted BOOLEAN, " +
                "link TEXT" +
                ");";

        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            System.out.println("Таблица 'user' создана или уже существует.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
