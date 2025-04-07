package com.bookstore;

public class Book {
    private int id;
    private String name;
    private double price;
    private String description;
    private Category category;
    private String coverPath;

    public Book(int id, String name, double price, String description, Category category, String coverPath) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.category = category;
        this.coverPath = coverPath;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public String getCoverPath() {
        return coverPath;
    }
}