package com.bookstore;

public abstract class User {
    protected String login;
    protected String password;
    protected String role;

    public User(String login, String password, String role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }

    public String getLogin() { return login; }
    public String getRole() { return role; }
    public boolean authenticate(String password) { return this.password.equals(password); }
}