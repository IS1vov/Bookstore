package com.bookstore;

public class Admin extends User {
    public Admin(String login, String password) {
        super(login, password, "Admin");
    }
}