package com.bookstore;

public class Book {
    private int id; // Для базы данных
    private String name;
    private double price;
    private String description;
    private Category category;

    public Book(int id, String name, double price, String description, Category category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public void setName(String name) { this.name = name; }
    public void setPrice(double price) { this.price = price; }
    public void setDescription(String description) { this.description = description; }

    // Update method
    public void update(String name, double price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }
}