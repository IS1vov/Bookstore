package com.bookstore;

public class Main {
    public static void main(String[] args) {
        BookStore store = new BookStore();
        new MainWindow(store);
    }
}