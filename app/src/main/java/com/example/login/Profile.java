package com.example.login;

public class Profile {
    public String name, email, date;

    public Profile() {} // Required for Firebase

    public Profile(String name, String email, String date) {
        this.name = name;
        this.email = email;
        this.date = date;
    }


    // Getters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDate() { return date; }
}