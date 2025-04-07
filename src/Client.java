package com.bookstore;

public class Client extends User {
    public Client(String login, String name, String password, String avatarPath) {
        super(login, name, password, avatarPath);
    }

    @Override
    public String getRole() {
        return "Client";
    }
}