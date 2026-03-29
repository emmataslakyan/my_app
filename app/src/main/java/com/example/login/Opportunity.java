package com.example.login;

public class Opportunity {
    private String id = "";
    private String title = "";
    private String description = "";
    private String url = "";
    private String source = "";
    private String format = "Online";
    private String category = "Program";
    private String cost = "Free";
    private String location = "";

    public Opportunity() {}

    // Inside Opportunity.java
    public Opportunity(String title, String url, String source) {
        this.title = title;
        this.url = url;
        this.source = source;
        // Set defaults so UI doesn't crash on nulls
        this.description = "";
        this.format = "Online/Hybrid";
        this.category = "Program";
        this.cost = "Contact for details";
    }

    // FIXED: Ensure this is setId (uppercase I)
    public void setId(String id) { this.id = (id != null) ? id : ""; }
    public String getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCost() { return cost; }
    public void setCost(String cost) { this.cost = cost; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}