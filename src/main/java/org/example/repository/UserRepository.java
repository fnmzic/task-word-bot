package org.example.repository;

import org.example.db.DatabaseManager;
import org.example.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    // Проверка существования пользователя
    public static boolean userExists(long chatId) {
        String sql = "SELECT COUNT(*) FROM \"user\" WHERE id = ?;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chatId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1) > 0; // Если COUNT > 0, пользователь существует
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Обновление UTM-ссылки пользователя
    public static void updateUserLink(long chatId, String link) {
        String sql = "UPDATE \"user\" SET link = ? WHERE id = ?;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.setLong(2, chatId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Добавление нового пользователя
    public static void insertUser(long chatId, boolean isPolicyAccepted, String link) {
        String sql = "INSERT INTO \"user\" (id, isPolicyAccepted, link) VALUES (?, ?, ?) " +
                "ON CONFLICT (id) DO NOTHING;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chatId);
            statement.setBoolean(2, isPolicyAccepted);
            statement.setString(3, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Обновление признака согласия на обработку данных
    public static void updatePolicyAcceptance(long chatId, boolean isPolicyAccepted) {
        String sql = "UPDATE \"user\" SET isPolicyAccepted = ? WHERE id = ?;";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, isPolicyAccepted);
            statement.setLong(2, chatId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Получение пользователя по chatId
    public static User getUser(long chatId) {
        String sql = "SELECT * FROM \"user\" WHERE id = ?;";
        User user = null;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, chatId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                user = new User(
                        resultSet.getLong("id"),
                        resultSet.getBoolean("isPolicyAccepted"),
                        resultSet.getString("link")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return user;
    }
}
