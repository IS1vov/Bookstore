package com.bookstore;

public abstract class User {
    protected String login;
    protected String name;
    protected String password;
    protected String avatarPath;
    protected String role;

    public User(String login, String name, String password, String avatarPath) {
        this.login = login;
        this.name = name;
        this.password = password;
        this.avatarPath = avatarPath;
        this.role = "Client";
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean authenticate(String password) {
        return this.password.equals(password);
    }
}