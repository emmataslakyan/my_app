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

    public Opportunity(String title, String description, String url, String format,
                      String category, String cost, String location) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.format = format;
        this.category = category;
        this.cost = cost;
        this.location = location;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCost() { return cost; }
    public void setCost(String cost) { this.cost = cost; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
