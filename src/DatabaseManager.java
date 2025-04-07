package com.bookstore;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
        String sql = "CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "description TEXT, " +
                "category TEXT NOT NULL)";
        Statement stmt = conn.createStatement();
        stmt.execute(sql);
    }

    public void saveBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (name, price, description, category) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, book.getName());
        pstmt.setDouble(2, book.getPrice());
        pstmt.setString(3, book.getDescription());
        pstmt.setString(4, book.getCategory().getName());
        pstmt.executeUpdate();
    }

    public ResultSet getBooks(String category) throws SQLException {
        String sql = "SELECT * FROM books WHERE category = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, category);
        return pstmt.executeQuery();
    }

    public void updateBook(int id, String name, double price, String description) throws SQLException {
        String sql = "UPDATE books SET name = ?, price = ?, description = ? WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, name);
        pstmt.setDouble(2, price);
        pstmt.setString(3, description);
        pstmt.setInt(4, id);
        pstmt.executeUpdate();
    }

    public void deleteBook(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
    }
}