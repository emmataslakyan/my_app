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
        ctx.put("photoPath", photoUri(r.getPhotoPath()));
        ctx.put("hasPhoto", !nz(r.getPhotoPath()).isEmpty());

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

    private static String photoUri(String path) {
        String p = nz(path);
        if (p.isEmpty()) return "";
        if (p.startsWith("file://") || p.startsWith("http://") || p.startsWith("https://")
                || p.startsWith("content://") || p.startsWith("data:")) {
            return p;
        }
        return "file://" + p;
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
