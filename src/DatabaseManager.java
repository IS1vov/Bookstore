package com.bookstore;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private Connection conn;

    public void connect() throws SQLException {
        String dbPath = "/Users/is/Desktop/Bookstore1/src/bookstore.db";
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        System.out.println("Подключение к базе данных: " + dbPath);
        conn.setAutoCommit(true);
    }

    public void close() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
            System.out.println("Соединение с базой данных закрыто");
        }
    }

    public void createTables() throws SQLException {
        Statement stmt = conn.createStatement();

        // Проверяем и создаём таблицу users, если её нет
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                "login TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "avatar_path TEXT, " +
                "role TEXT NOT NULL DEFAULT 'Client')");

        // Проверяем и создаём таблицу categories, если её нет
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS categories (" +
                "name TEXT PRIMARY KEY)");

        // Проверяем и создаём таблицу books, если её нет
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "description TEXT, " +
                "category_name TEXT, " +
                "cover_path TEXT, " +
                "FOREIGN KEY (category_name) REFERENCES categories(name))");

        // Проверяем и создаём таблицу reviews, если её нет
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "book_id INTEGER NOT NULL, " +
                "user_login TEXT NOT NULL, " +
                "text TEXT NOT NULL, " +
                "parent_id INTEGER, " +
                "likes INTEGER DEFAULT 0, " +
                "dislikes INTEGER DEFAULT 0, " +
                "FOREIGN KEY (book_id) REFERENCES books(id), " +
                "FOREIGN KEY (user_login) REFERENCES users(login), " +
                "FOREIGN KEY (parent_id) REFERENCES reviews(id))");

        // Проверяем и создаём таблицу reactions, если её нет
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_login TEXT NOT NULL, " +
                "review_id INTEGER NOT NULL, " +
                "reaction TEXT NOT NULL, " +
                "FOREIGN KEY (user_login) REFERENCES users(login), " +
                "FOREIGN KEY (review_id) REFERENCES reviews(id))");

        // Проверяем и создаём таблицу book_reactions, если её нет
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS book_reactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_login TEXT NOT NULL, " +
                "book_id INTEGER NOT NULL, " +
                "reaction TEXT NOT NULL, " +
                "FOREIGN KEY (user_login) REFERENCES users(login), " +
                "FOREIGN KEY (book_id) REFERENCES books(id))");
    }

    public boolean categoryExists(String name) throws SQLException {
        String query = "SELECT COUNT(*) FROM categories WHERE name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public void saveCategory(String name) throws SQLException {
        String query = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            System.out.println("Категория сохранена: " + name);
        }
    }

    public ResultSet getBooks(String categoryName) throws SQLException {
        String query = "SELECT * FROM books WHERE category_name = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, categoryName);
        return stmt.executeQuery();
    }

    public void saveBook(Book book) throws SQLException {
        String query = "INSERT INTO books (name, price, description, category_name, cover_path) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, book.getName());
            stmt.setDouble(2, book.getPrice());
            stmt.setString(3, book.getDescription());
            stmt.setString(4, book.getCategory().getName());
            stmt.setString(5, book.getCoverPath());
            stmt.executeUpdate();
        }
    }

    public void updateBook(int id, String name, double price, String description, String coverPath) throws SQLException {
        String query = "UPDATE books SET name = ?, price = ?, description = ?, cover_path = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setDouble(2, price);
            stmt.setString(3, description);
            stmt.setString(4, coverPath);
            stmt.setInt(5, id);
            stmt.executeUpdate();
        }
    }

    public void deleteBook(int id) throws SQLException {
        String query = "DELETE FROM books WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public User findUser(String login) throws SQLException {
        String query = "SELECT * FROM users WHERE login = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new Client(rs.getString("login"), rs.getString("name"), rs.getString("password"), rs.getString("avatar_path"));
                user.setRole(rs.getString("role"));
                System.out.println("Найден пользователь: " + login + ", пароль в базе: " + rs.getString("password") + ", роль: " + rs.getString("role"));
                return user;
            }
            System.out.println("Пользователь " + login + " не найден в базе");
            return null;
        }
    }

    public void registerUser(String login, String name, String password, String avatarPath) throws SQLException {
        String query = "INSERT INTO users (login, name, password, avatar_path, role) VALUES (?, ?, ?, ?, 'Client')";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            stmt.setString(2, name);
            stmt.setString(3, password);
            stmt.setString(4, avatarPath);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Добавлено строк в таблицу users: " + rowsAffected);
        }
    }

    public void registerAdmin(String login, String name, String password, String avatarPath) throws SQLException {
        String query = "INSERT INTO users (login, name, password, avatar_path, role) VALUES (?, ?, ?, ?, 'Admin')";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            stmt.setString(2, name);
            stmt.setString(3, password);
            stmt.setString(4, avatarPath);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Добавлено строк в таблицу users (Admin): " + rowsAffected);
        }
    }

    public void updateUserRole(String login, String role) throws SQLException {
        String query = "UPDATE users SET role = ? WHERE login = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, role);
            stmt.setString(2, login);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Обновлена роль для " + login + " на " + role + ", строк изменено: " + rowsAffected);
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                User user = new Client(rs.getString("login"), rs.getString("name"), rs.getString("password"), rs.getString("avatar_path"));
                user.setRole(rs.getString("role"));
                users.add(user);
            }
        }
        return users;
    }

    public void removeUser(String login) throws SQLException {
        String query = "DELETE FROM users WHERE login = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, login);
            stmt.executeUpdate();
        }
    }

    public void saveReview(int bookId, String userLogin, String text, Integer parentId) throws SQLException {
        String query = "INSERT INTO reviews (book_id, user_login, text, parent_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            stmt.setString(2, userLogin);
            stmt.setString(3, text);
            if (parentId != null) {
                stmt.setInt(4, parentId);
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.executeUpdate();
        }
    }

    public List<Review> getReviews(int bookId) {
        try {
            List<Review> reviews = new ArrayList<>();
            String query = "SELECT * FROM reviews WHERE book_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, bookId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Review review = new Review(rs.getInt("id"), rs.getInt("book_id"), rs.getString("user_login"),
                            rs.getString("text"), rs.getInt("likes"), rs.getInt("dislikes"));
                    if (rs.getObject("parent_id") != null) {
                        review.setParentId(rs.getInt("parent_id"));
                    }
                    reviews.add(review);
                }
            }
            for (Review review : reviews) {
                review.setReplies(getReplies(review.getId(), reviews));
            }
            return reviews;
        } catch (SQLException e) {
            System.err.println("Ошибка получения отзывов: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Review> getReplies(int parentId, List<Review> allReviews) {
        List<Review> replies = new ArrayList<>();
        for (Review review : allReviews) {
            if (review.getParentId() != null && review.getParentId() == parentId) {
                replies.add(review);
            }
        }
        return replies;
    }

    public void saveReaction(String userLogin, int reviewId, String reaction) throws SQLException {
        String query = "INSERT INTO reactions (user_login, review_id, reaction) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, reviewId);
            stmt.setString(3, reaction);
            stmt.executeUpdate();
        }
    }

    public String getUserReaction(String userLogin, int reviewId) throws SQLException {
        String query = "SELECT reaction FROM reactions WHERE user_login = ? AND review_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, reviewId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("reaction") : null;
        }
    }

    public void updateReviewLikes(int reviewId, int likes, int dislikes) throws SQLException {
        String query = "UPDATE reviews SET likes = ?, dislikes = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, likes);
            stmt.setInt(2, dislikes);
            stmt.setInt(3, reviewId);
            stmt.executeUpdate();
        }
    }

    public void saveBookReaction(String userLogin, int bookId, String reaction) throws SQLException {
        String query = "INSERT INTO book_reactions (user_login, book_id, reaction) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, bookId);
            stmt.setString(3, reaction);
            stmt.executeUpdate();
        }
    }

    public String getUserBookReaction(String userLogin, int bookId) throws SQLException {
        String query = "SELECT reaction FROM book_reactions WHERE user_login = ? AND book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userLogin);
            stmt.setInt(2, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("reaction") : null;
        }
    }

    public int getBookRating(int bookId) throws SQLException {
        String query = "SELECT SUM(CASE WHEN reaction = 'LIKE' THEN 1 ELSE -1 END) AS rating " +
                "FROM book_reactions WHERE book_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("rating") : 0;
        }
    }
}