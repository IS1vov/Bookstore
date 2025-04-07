package com.bookstore;

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
        initializeCategories();
        initializeUsers();
    }

    private void initializeCategories() {
        categories.add(new Category("Fiction"));
        categories.add(new Category("Non-Fiction"));
        categories.add(new Category("Science"));
        categories.add(new Category("Fantasy"));
        categories.add(new Category("Mystery"));
        categories.add(new Category("Romance"));
        categories.add(new Category("History"));
    }

    private void initializeUsers() {
        users.add(new Admin("admin", "admin123"));
        users.add(new Client("client", "client123"));
    }

    // CRUD for Categories
    public void createCategory(String name) { categories.add(new Category(name)); }
    public List<Category> readCategories() { return categories; }
    public void deleteCategory(int index) { categories.remove(index); }

    // User management
    public User findUser(String login) {
        return users.stream().filter(u -> u.getLogin().equals(login)).findFirst().orElse(null);
    }

    public DatabaseManager getDb() { return db; }
}