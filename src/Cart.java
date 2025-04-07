package com.bookstore;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<Book> books;

    public Cart() {
        books = new ArrayList<>();
    }

    public void addBook(Book book) {
        books.add(book);
    }

    public void removeBook(int index) {
        books.remove(index);
    }

    public List<Book> getBooks() {
        return books;
    }

    public void clear() {
        books.clear();
    }
}