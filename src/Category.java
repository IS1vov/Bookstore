package com.bookstore;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String name;
    private List<Book> books;

    public Category(String name) {
        this.name = name;
        this.books = new ArrayList<>();
    }

    // CRUD for Books
    public void createBook(int id, String name, double price, String description) {
        books.add(new Book(id, name, price, description, this));
    }

    public List<Book> readBooks() {
        return books;
    }

    public void updateBook(int index, String name, double price, String description) {
        books.get(index).update(name, price, description);
    }

    public void deleteBook(int index) {
        books.remove(index);
    }

    public String getName() { return name; }
}