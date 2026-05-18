package com.example.login;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Top-level program filters — mirror the chips on https://greenwich.am/programs.
 *
 * Each filter's {@code keywords} are matched (case-insensitive, substring) against the
 * program's {@code categories} field. {@link #ALL} accepts every program.
 */
public final class ProgramFilter {

    public final String id;
    public final String label;
    private final List<String> keywords;

    private ProgramFilter(String id, String label, String... keywords) {
        this.id = id;
        this.label = label;
        this.keywords = Arrays.asList(keywords);
    }

    /** "All Programs" — passes everything. */
    public static final ProgramFilter ALL =
            new ProgramFilter("all", "All Programs");

    /**
     * Ordered list of top-level filters as they appear on the website's chip row.
     * Subcategories (e.g. Bachelor, Master, Summer, Funded) are intentionally
     * omitted from v1.
     */
    public static final List<ProgramFilter> ALL_FILTERS = Collections.unmodifiableList(Arrays.asList(
            ALL,
            new ProgramFilter("education",    "Education",    "education"),
            new ProgramFilter("scholarship",  "Scholarships", "scholarship"),
            new ProgramFilter("fellowship",   "Fellowships",  "fellowship"),
            new ProgramFilter("training",     "Training",     "training"),
            new ProgramFilter("grant",        "Grants",       "grant"),
            new ProgramFilter("internship",   "Internships",  "internship"),
            new ProgramFilter("conference",   "Conferences",  "conference"),
            new ProgramFilter("online",       "Online",       "online", "webinar", "mooc"),
            new ProgramFilter("volunteering", "Volunteering", "volunteering"),
            new ProgramFilter("event",        "Events",       "event", "festival", "competition", "summit", "forum", "seminar"),
            new ProgramFilter("award",        "Awards",       "award")
    ));

    /** True if this program's categories match any of this filter's keywords. */
    public boolean matches(Program program) {
        if (this == ALL || keywords.isEmpty()) return true;
        if (program == null) return false;
        String haystack = (program.categoriesText == null ? "" : program.categoriesText).toLowerCase();
        if (haystack.isEmpty()) return false;
        for (String kw : keywords) {
            if (haystack.contains(kw)) return true;
        }
        return false;
    }
}
