package com.example.login;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResumeDataMapper {

    private ResumeDataMapper() {}

    public static Map<String, Object> toContext(Resume r) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        if (r == null) return ctx;

        // Header
        ctx.put("name", nz(r.getName()));
        ctx.put("title", nz(r.getTitle()));
        ctx.put("email", nz(r.getEmail()));
        ctx.put("phone", nz(r.getPhone()));
        ctx.put("address", nz(r.getAddress()));
        String photoData = photoData(r.getPhotoPath());
        ctx.put("photoPath", photoData);
        ctx.put("hasPhoto", !photoData.isEmpty());

        // Education
        List<Map<String, Object>> education = new ArrayList<>();
        for (EducationEntry entry : ResumeEntries.parseEducation(r.getEducationJson())) {
            if (entry.isBlank()) continue;
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("schoolName", nz(entry.schoolName));
            e.put("schoolLocation", nz(entry.schoolLocation));
            e.put("schoolDate", nz(entry.schoolDate));
            e.put("degree", nz(entry.degree));
            e.put("schoolDescription", nz(entry.schoolDescription));
            e.put("schoolDescriptionBullets", MustacheRenderer.splitLines(entry.schoolDescription));
            education.add(e);
        }
        ctx.put("education", education);
        ctx.put("hasEducation", !education.isEmpty());

        // Experience
        List<Map<String, Object>> experience = new ArrayList<>();
        for (ExperienceEntry entry : ResumeEntries.parseExperience(r.getExperienceJson())) {
            if (entry.isBlank()) continue;
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("expOrgName", nz(entry.expOrgName));
            x.put("expPosition", nz(entry.expPosition));
            x.put("expLocation", nz(entry.expLocation));
            x.put("expDate", nz(entry.expDate));
            x.put("expBullets", MustacheRenderer.splitLines(entry.expBullets));
            experience.add(x);
        }
        ctx.put("experience", experience);
        ctx.put("hasExperience", !experience.isEmpty());

        // Volunteering
        List<Map<String, Object>> volunteering = new ArrayList<>();
        if (anyNotBlank(r.getVolOrgName(), r.getVolPosition(),
                r.getVolLocation(), r.getVolDate(), r.getVolBullets())) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("volOrgName", nz(r.getVolOrgName()));
            v.put("volPosition", nz(r.getVolPosition()));
            v.put("volLocation", nz(r.getVolLocation()));
            v.put("volDate", nz(r.getVolDate()));
            v.put("volBullets", MustacheRenderer.splitLines(r.getVolBullets()));
            volunteering.add(v);
        }
        ctx.put("volunteering", volunteering);

        // Projects
        List<Map<String, Object>> projects = new ArrayList<>();
        if (anyNotBlank(r.getProjectName(), r.getProjectRole(),
                r.getProjectDate(), r.getProjectBullets())) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("projectName", nz(r.getProjectName()));
            p.put("projectRole", nz(r.getProjectRole()));
            p.put("projectDate", nz(r.getProjectDate()));
            p.put("projectBullets", MustacheRenderer.splitLines(r.getProjectBullets()));
            projects.add(p);
        }
        ctx.put("projects", projects);

        // Skills / languages — comma-or-newline separated → list of strings.
        List<String> skillList = splitCsv(r.getSkills());
        List<String> langList = splitCsv(r.getLanguages());
        ctx.put("skills", skillList);
        ctx.put("languages", langList);
        ctx.put("hasSkills", !skillList.isEmpty());
        ctx.put("hasLanguages", !langList.isEmpty());

        return ctx;
    }

    private static String photoData(String path) {
        String p = nz(path);
        if (p.isEmpty()) return "";
        if (p.startsWith("data:")) return p;
        if (p.startsWith("http://") || p.startsWith("https://")) return p;
        String filePath = p.startsWith("file://") ? p.substring(7) : p;
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) return "";
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int n;
            while ((n = in.read(tmp)) != -1) buf.write(tmp, 0, n);
            String mime = filePath.endsWith(".png") ? "image/png" : "image/jpeg";
            return "data:" + mime + ";base64,"
                    + android.util.Base64.encodeToString(buf.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private static List<String> splitCsv(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String part : raw.split("[,\\n]")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static boolean anyNotBlank(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return true;
        return false;
    }
}
