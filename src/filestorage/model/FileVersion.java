package filestorage.model;

import java.time.Instant;

public class FileVersion {
    private final int versionNumber;
    private final String content;
    private final String uploadedBy;
    private final Instant createdAt;

    public FileVersion(int versionNumber, String content, String uploadedBy) {
        this.versionNumber = versionNumber;
        this.content = content;
        this.uploadedBy = uploadedBy;
        this.createdAt = Instant.now();
    }

    public int getVersionNumber() { return versionNumber; }
    public String getContent() { return content; }
    public String getUploadedBy() { return uploadedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public int getSizeBytes() { return content.getBytes().length; }

    @Override
    public String toString() { return "v" + versionNumber + " (" + getSizeBytes() + "B, by " + uploadedBy + ")"; }
}
