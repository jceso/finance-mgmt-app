package com.example.financemanagement.models;

public class Category {
    private String type;
    private String name;
    private String icon;
    private Boolean fav;

    public Category(String type, String name, String icon) {
        this.type = type;
        this.name = name;
        this.icon = icon;
    }

    public String getType() { return type; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public Boolean getFav() { return fav; }
}

