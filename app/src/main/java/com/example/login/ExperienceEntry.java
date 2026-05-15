package com.example.login;

import org.json.JSONException;
import org.json.JSONObject;

public class ExperienceEntry {
    public String expOrgName = "";
    public String expPosition = "";
    public String expLocation = "";
    public String expDate = "";
    public String expBullets = "";

    public ExperienceEntry() {}

    public static ExperienceEntry fromJson(JSONObject o) {
        ExperienceEntry e = new ExperienceEntry();
        e.expOrgName = o.optString("expOrgName", "");
        e.expPosition = o.optString("expPosition", "");
        e.expLocation = o.optString("expLocation", "");
        e.expDate = o.optString("expDate", "");
        e.expBullets = o.optString("expBullets", "");
        return e;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("expOrgName", expOrgName == null ? "" : expOrgName);
        o.put("expPosition", expPosition == null ? "" : expPosition);
        o.put("expLocation", expLocation == null ? "" : expLocation);
        o.put("expDate", expDate == null ? "" : expDate);
        o.put("expBullets", expBullets == null ? "" : expBullets);
        return o;
    }

    public boolean isBlank() {
        return isEmpty(expOrgName) && isEmpty(expPosition) && isEmpty(expLocation)
                && isEmpty(expDate) && isEmpty(expBullets);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
