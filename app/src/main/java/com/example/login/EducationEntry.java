package com.example.login;

import org.json.JSONException;
import org.json.JSONObject;

public class EducationEntry {
    public String schoolName = "";
    public String schoolLocation = "";
    public String schoolDate = "";
    public String degree = "";
    public String schoolDescription = "";

    public EducationEntry() {}

    public static EducationEntry fromJson(JSONObject o) {
        EducationEntry e = new EducationEntry();
        e.schoolName = o.optString("schoolName", "");
        e.schoolLocation = o.optString("schoolLocation", "");
        e.schoolDate = o.optString("schoolDate", "");
        e.degree = o.optString("degree", "");
        e.schoolDescription = o.optString("schoolDescription", "");
        return e;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("schoolName", schoolName == null ? "" : schoolName);
        o.put("schoolLocation", schoolLocation == null ? "" : schoolLocation);
        o.put("schoolDate", schoolDate == null ? "" : schoolDate);
        o.put("degree", degree == null ? "" : degree);
        o.put("schoolDescription", schoolDescription == null ? "" : schoolDescription);
        return o;
    }

    public boolean isBlank() {
        return isEmpty(schoolName) && isEmpty(schoolLocation) && isEmpty(schoolDate)
                && isEmpty(degree) && isEmpty(schoolDescription);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
