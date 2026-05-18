package com.example.login;

public class Program {
    public final String title;
    public final String imageUrl;       // may be empty
    public final String programUrl;     // detail page (absolute URL)
    public final String meta;           // short display blurb (categories / source / date), may be empty
    public final String categoriesText; // raw category labels joined with spaces, for filtering; may be empty
    public final String description;    // free-form program description (used by Age filter)
    public final long deadlineMillis;   // end-of-day deadline parsed from description, or 0 if absent / "no deadline"

    public Program(String title, String imageUrl, String programUrl, String meta,
                   String categoriesText, String description, long deadlineMillis) {
        this.title = title == null ? "" : title;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.programUrl = programUrl == null ? "" : programUrl;
        this.meta = meta == null ? "" : meta;
        this.categoriesText = categoriesText == null ? "" : categoriesText;
        this.description = description == null ? "" : description;
        this.deadlineMillis = deadlineMillis;
    }
}
