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

    public void createBook(int id, String name, double price, String description, String coverPath) {
        books.add(new Book(id, name, price, description, this, coverPath));
    }

    public List<Book> readBooks() {
        return books;
    }

    public void updateBook(int index, String name, double price, String description) {
        books.get(index).update(name, price, description, books.get(index).getCoverPath());
    }

    public void deleteBook(int index) {
        books.remove(index);
    }

    public String getName() { return name; }
}