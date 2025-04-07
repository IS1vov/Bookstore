package com.bookstore;

public class Book {
    private int id;
    private String name;
    private double price;
    private String description;
    private Category category;
    private String coverPath; // Путь к файлу обложки

    public Book(int id, String name, double price, String description, Category category, String coverPath) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
        this.coverPath = coverPath;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public String getCoverPath() { return coverPath; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setDescription(String description) { this.description = description; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    // Update method
    public void update(String name, double price, String description, String coverPath) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.coverPath = coverPath;
    }
}