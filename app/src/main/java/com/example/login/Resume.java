package com.example.login;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "resumes")
public class Resume {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String email;
    private String date;

    // Constructor used by the Editor and Adapter
    public Resume(String title, String email, String date) {
        this.title = title;
        this.email = email;
        this.date = date;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public String getEmail() { return email; }
    public String getDate() { return date; }
}