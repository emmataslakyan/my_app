package com.example.login;

public class ResumeTemplate {
    public static final String DEFAULT_ID = "default";

    private final String id;
    private final String name;
    private final long version;
    private final String thumbnailUrl;
    private final String storagePath;
    private final boolean bundled;

    public ResumeTemplate(String id, String name, long version,
                          String thumbnailUrl, String storagePath, boolean bundled) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.thumbnailUrl = thumbnailUrl;
        this.storagePath = storagePath;
        this.bundled = bundled;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getVersion() { return version; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getStoragePath() { return storagePath; }
    public boolean isBundled() { return bundled; }
}
