package com.example.login;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "resumes")
public class Resume {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String email;
    private String date;
    private String name;
    private String phone;
    private String address;

    // Education + Experience are JSON-encoded lists of entries.
    // See EducationEntry / ExperienceEntry and ResumeEntries.
    private String educationJson;
    private String experienceJson;

    // Volunteering Fields
    private String volOrgName;
    private String volPosition;
    private String volLocation;
    private String volDate;
    private String volBullets;

    // Skills, Projects, and Languages
    private String skills;
    private String projectName;
    private String projectRole;
    private String projectDate;
    private String projectBullets;
    private String languages;

    // Photo
    @ColumnInfo(name = "photo_path")
    private String photoPath;

    @ColumnInfo(name = "template_id", defaultValue = "default")
    private String templateId = "default";

    public Resume() {}

    @Ignore
    public Resume(String title, String email, String date) {
        this.title = title;
        this.email = email;
        this.date = date;
    }

    // --- Core Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // --- Photo ---
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    // --- Template ---
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    // --- Education / Experience (JSON lists) ---
    public String getEducationJson() { return educationJson; }
    public void setEducationJson(String educationJson) { this.educationJson = educationJson; }
    public String getExperienceJson() { return experienceJson; }
    public void setExperienceJson(String experienceJson) { this.experienceJson = experienceJson; }

    // --- Volunteering ---
    public String getVolOrgName() { return volOrgName; }
    public void setVolOrgName(String volOrgName) { this.volOrgName = volOrgName; }
    public String getVolPosition() { return volPosition; }
    public void setVolPosition(String volPosition) { this.volPosition = volPosition; }
    public String getVolLocation() { return volLocation; }
    public void setVolLocation(String volLocation) { this.volLocation = volLocation; }
    public String getVolDate() { return volDate; }
    public void setVolDate(String volDate) { this.volDate = volDate; }
    public String getVolBullets() { return volBullets; }
    public void setVolBullets(String volBullets) { this.volBullets = volBullets; }

    // --- Skills, Projects, Languages ---
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectRole() { return projectRole; }
    public void setProjectRole(String projectRole) { this.projectRole = projectRole; }
    public String getProjectDate() { return projectDate; }
    public void setProjectDate(String projectDate) { this.projectDate = projectDate; }
    public String getProjectBullets() { return projectBullets; }
    public void setProjectBullets(String projectBullets) { this.projectBullets = projectBullets; }
    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }
}