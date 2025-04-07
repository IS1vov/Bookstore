package com.bookstore;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookStore {
    private List<Category> categories;
    private List<User> users;
    private DatabaseManager db;

    public BookStore() {
        categories = new ArrayList<>();
        users = new ArrayList<>();
        db = new DatabaseManager();

        // Инициализация категорий
        String[] categoryNames = {"Fiction", "Non-Fiction", "Science", "History", "Fantasy", "Mystery", "Romance"};
        for (String name : categoryNames) {
            categories.add(new Category(name));
        }

        // Добавляем фиксированного админа с аватаркой
        users.add(new Admin("admin", "Administrator", "admin123", "avatars/ava.jpg"));
    }

    public List<Category> readCategories() {
        return categories;
    }

    public User findUser(String login) {
        // Сначала проверяем фиксированного админа
        for (User user : users) {
            if (user.getLogin().equals(login)) {
                return user;
            }
        }
        // Затем проверяем базу данных
        try {
            ResultSet rs = db.getUsers();
            while (rs.next()) {
                String dbLogin = rs.getString("login");
                if (dbLogin.equals(login)) {
                    String name = rs.getString("name");
                    String password = rs.getString("password");
                    String avatarPath = rs.getString("avatar_path");
                    return new Client(dbLogin, name, password, avatarPath); // Не добавляем в users, чтобы избежать дублирования
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return null;
    }

    public List<User> getAllUsers() {
        List<User> allUsers = new ArrayList<>();
        allUsers.add(users.get(0)); // Добавляем только админа из фиксированного списка
        try {
            ResultSet rs = db.getUsers();
            while (rs.next()) {
                String login = rs.getString("login");
                String name = rs.getString("name");
                String password = rs.getString("password");
                String avatarPath = rs.getString("avatar_path");
                allUsers.add(new Client(login, name, password, avatarPath));
            }
        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
        return allUsers;
    }

    public void registerUser(String login, String name, String password, String avatarPath) throws SQLException {
        db.saveUser(login, name, password, avatarPath);
        // Не добавляем в users, чтобы список содержал только админа
    }

    public void removeUser(String login) throws SQLException {
        db.deleteUser(login);
    }

    public DatabaseManager getDb() {
        return db;
    }
}