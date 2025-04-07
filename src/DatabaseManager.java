package com.bookstore;

import java.sql.*;
import java.io.*;

public class DatabaseManager {
    private Connection conn;

    public void connect() throws SQLException {
        String url = "jdbc:sqlite:bookstore.db";
        conn = DriverManager.getConnection(url);
    }

    public void disconnect() throws SQLException {
        if (conn != null) conn.close();
    }

    public void createTables() throws SQLException {
        String booksTable = "CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "description TEXT, " +
                "category TEXT NOT NULL, " +
                "cover_path TEXT)";
        String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "login TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "password TEXT NOT NULL, " +
                "avatar_path TEXT)";
        Statement stmt = conn.createStatement();
        stmt.execute(booksTable);
        stmt.execute(usersTable);
    }

    public void saveBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (name, price, description, category, cover_path) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, book.getName());
        pstmt.setDouble(2, book.getPrice());
        pstmt.setString(3, book.getDescription());
        pstmt.setString(4, book.getCategory().getName());
        pstmt.setString(5, book.getCoverPath());
        pstmt.executeUpdate();
    }

    public ResultSet getBooks(String category) throws SQLException {
        String sql = "SELECT * FROM books WHERE category = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, category);
        return pstmt.executeQuery();
    }

    public void updateBook(int id, String name, double price, String description, String coverPath) throws SQLException {
        String sql = "UPDATE books SET name = ?, price = ?, description = ?, cover_path = ? WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setDouble(2, price);
        pstmt.setString(3, description);
        pstmt.setString(4, coverPath);
        pstmt.setInt(5, id);
        pstmt.executeUpdate();
    }

    public void deleteBook(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
    }

    public void saveUser(String login, String name, String password, String avatarPath) throws SQLException {
        String sql = "INSERT INTO users (login, name, password, avatar_path) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, login);
        pstmt.setString(2, name);
        pstmt.setString(3, password);
        pstmt.setString(4, avatarPath);
        pstmt.executeUpdate();
    }

    public ResultSet getUsers() throws SQLException {
        String sql = "SELECT * FROM users";
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(sql); // Исправлено: добавлен аргумент sql
    }

    public void deleteUser(String login) throws SQLException {
        String sql = "DELETE FROM users WHERE login = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, login);
        pstmt.executeUpdate();
    }
}