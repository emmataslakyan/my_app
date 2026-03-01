package com.example.login;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ResumeDao {
    @Insert
    void insert(Resume resume);

    @Delete
    void delete(Resume resume);

    // FIX: Changed 'resume_table' to 'resumes' to match standard naming
    // If your Resume class doesn't have a tableName, use 'Resume' here
    @Query("SELECT * FROM resumes ORDER BY id DESC")
    List<Resume> getAllResumes();
}