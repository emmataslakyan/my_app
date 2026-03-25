package com.example.login;

public class Opportunity {
    private String id;
    private String title;
    private String description;
    private String url;
    private String source;
    private String format;
    private String category;
    private String cost;
    private String deadline;
    private String location;
    private String imageUrl;

    public Opportunity() {}

    // Getters with basic null safety
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title != null ? title : "No Title"; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description != null ? description : ""; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url != null ? url : ""; }
    public void setUrl(String url) { this.url = url; }

    public String getFormat() { return format != null ? format : "Other"; }
    public void setFormat(String format) { this.format = format; }

    public String getCategory() { return category != null ? category : "General"; }
    public void setCategory(String category) { this.category = category; }

    public String getCost() { return cost != null ? cost : "Free"; }
    public void setCost(String cost) { this.cost = cost; }

    public String getLocation() { return location != null ? location : ""; }
    public void setLocation(String location) { this.location = location; }

    // Other getters/setters...
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}