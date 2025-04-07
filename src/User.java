package com.bookstore;

public abstract class User {
    protected String login;
    protected String name;
    protected String password;
    protected String avatarPath;

    public User(String login, String name, String password, String avatarPath) {
        this.login = login;
        this.name = name;
        this.password = password;
        this.avatarPath = avatarPath;
    }

    public abstract String getRole();

    public boolean authenticate(String password) {
        return this.password.equals(password);
    }

    public String getLogin() { return login; }
    public String getName() { return name; }
    public String getAvatarPath() { return avatarPath; }
}