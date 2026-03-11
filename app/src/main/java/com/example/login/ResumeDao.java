package com.example.login;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ResumeDao {
    @Insert
    long insert(Resume resume); // Must return long to get the new ID

    @Update
    void update(Resume resume);

    @Query("SELECT * FROM resumes WHERE id = :id")
    Resume getResumeById(int id);

    @Query("SELECT * FROM resumes")
    List<Resume> getAllResumes();

    @Delete
    void delete(Resume resume);
}