package com.bookstore;

public class Admin extends User {
    public Admin(String login, String name, String password, String avatarPath) {
        super(login, name, password, avatarPath);
    }

    @Override
    public String getRole() {
        return "Admin";
    }
}