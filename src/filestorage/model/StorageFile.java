package filestorage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageFile extends FileSystemItem {
    private final List<FileVersion> versions = new ArrayList<>();

    public StorageFile(String id, String name, String ownerId, String parentFolderId) {
        super(id, name, ownerId, parentFolderId);
    }

    // All four methods are synchronized on this StorageFile instance --
    // one monitor per file. InMemoryFileStorageService.uploadNewVersion
    // wraps its whole read-latest-then-add-then-prune sequence in a
    // `synchronized (file)` block using this same instance as the lock,
    // so that compound operation is atomic with respect to these
    // individual methods too (Java monitors are reentrant), closing the
    // race where two concurrent uploads to the same file could compute
    // the same "next version number" or interleave a prune mid-add.
    public synchronized void addVersion(FileVersion version) { versions.add(version); }
    public synchronized List<FileVersion> getVersions() { return Collections.unmodifiableList(new ArrayList<>(versions)); }
    public synchronized FileVersion getLatestVersion() { return versions.get(versions.size() - 1); }
    public synchronized void setVersions(List<FileVersion> trimmedVersions) {
        versions.clear();
        versions.addAll(trimmedVersions);
    }

    @Override
    public String getType() { return "FILE"; }

    @Override
    public String toString() { return "File{" + getId() + ", " + getName() + ", versions=" + versions.size() + '}'; }
}
