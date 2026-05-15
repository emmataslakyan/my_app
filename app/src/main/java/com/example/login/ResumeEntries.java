package com.example.login;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ResumeEntries {

    private ResumeEntries() {}

    public static List<EducationEntry> parseEducation(String json) {
        List<EducationEntry> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(EducationEntry.fromJson(o));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static List<ExperienceEntry> parseExperience(String json) {
        List<ExperienceEntry> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(ExperienceEntry.fromJson(o));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static String serializeEducation(List<EducationEntry> entries) {
        JSONArray arr = new JSONArray();
        if (entries != null) {
            for (EducationEntry e : entries) {
                if (e == null) continue;
                try {
                    arr.put(e.toJson());
                } catch (JSONException ignored) {
                }
            }
        }
        return arr.toString();
    }

    public static String serializeExperience(List<ExperienceEntry> entries) {
        JSONArray arr = new JSONArray();
        if (entries != null) {
            for (ExperienceEntry e : entries) {
                if (e == null) continue;
                try {
                    arr.put(e.toJson());
                } catch (JSONException ignored) {
                }
            }
        }
        return arr.toString();
    }
}
