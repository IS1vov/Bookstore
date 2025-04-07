package com.bookstore;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookStore {
    private DatabaseManager db;

    public BookStore() {
        db = new DatabaseManager();
        try {
            db.connect();
            db.createTables();
            User admin = findUser("admin");
            if (admin == null) {
                System.out.println("Создаём пользователя admin...");
                try {
                    db.registerAdmin("admin", "Admin", "admin123", "avatars/ava.jpg");
                    admin = findUser("admin");
                    if (admin != null) {
                        System.out.println("Админ успешно создан с логином: admin, паролем: admin123, роль: " + admin.getRole());
                    } else {
                        System.out.println("Ошибка: Админ не найден после попытки создания!");
                    }
                } catch (SQLException e) {
                    System.err.println("Ошибка при создании админа: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Админ уже существует: " + admin.getName() + ", роль: " + admin.getRole());
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при инициализации базы данных: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public DatabaseManager getDb() {
        return db;
    }

    public User findUser(String login) {
        try {
            return db.findUser(login);
        } catch (SQLException e) {
            System.err.println("Ошибка поиска пользователя: " + e.getMessage());
            return null;
        }
    }

    public void registerUser(String login, String name, String password, String avatarPath) throws SQLException {
        db.registerUser(login, name, password, avatarPath);
    }

    public List<User> getAllUsers() {
        try {
            return db.getAllUsers();
        } catch (SQLException e) {
            System.err.println("Ошибка получения пользователей: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void removeUser(String login) throws SQLException {
        db.removeUser(login);
    }

    public List<Category> readCategories() {
        List<Category> categories = new ArrayList<>();
        String[] categoryNames = {"Fiction", "Non-Fiction", "Science", "Fantasy", "Mystery", "Romance", "History"};
        try {
            for (String name : categoryNames) {
                if (!db.categoryExists(name)) {
                    db.saveCategory(name);
                }
                categories.add(new Category(name));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при работе с категориями в базе данных: " + e.getMessage());
            for (String name : categoryNames) {
                if (!categories.stream().anyMatch(c -> c.getName().equals(name))) {
                    categories.add(new Category(name));
                }
            }
        }
        System.out.println("Загружено категорий: " + categories.size());
        return categories;
    }

    public void addReview(int bookId, String userLogin, String text, Integer parentId) throws SQLException {
        db.saveReview(bookId, userLogin, text, parentId);
    }

    public List<Review> getReviews(int bookId) {
        return db.getReviews(bookId);
    }

}