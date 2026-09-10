package filestorage.service;

import filestorage.model.FileSystemItem;
import filestorage.model.FileVersion;
import filestorage.model.Folder;
import filestorage.model.PermissionLevel;
import filestorage.model.StorageFile;
import filestorage.strategy.VersionRetentionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryFileStorageService implements FileStorageService {
    private final Map<String, FileSystemItem> items = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public Folder createFolder(String ownerId, String name, String parentFolderId) {
        Folder folder = new Folder("FLD-" + idCounter.getAndIncrement(), name, ownerId, parentFolderId);
        items.put(folder.getId(), folder);
        linkToParent(folder, parentFolderId);
        return folder;
    }

    @Override
    public StorageFile uploadFile(String ownerId, String name, String parentFolderId, String content) throws PermissionDeniedException {
        requireEditAccess(parentFolderId, ownerId);
        StorageFile file = new StorageFile("FILE-" + idCounter.getAndIncrement(), name, ownerId, parentFolderId);
        file.addVersion(new FileVersion(1, content, ownerId));
        items.put(file.getId(), file);
        linkToParent(file, parentFolderId);
        return file;
    }

    @Override
    public StorageFile uploadNewVersion(String fileId, String uploaderId, String content, VersionRetentionStrategy retentionStrategy) throws PermissionDeniedException {
        StorageFile file = requireFile(fileId);
        if (!file.hasAtLeast(uploaderId, PermissionLevel.EDITOR)) {
            throw new PermissionDeniedException(uploaderId + " does not have EDITOR access to " + fileId);
        }
        // Synchronize the whole read-latest -> add -> prune sequence on
        // the file itself: without this, two concurrent uploads to the
        // same file could both read the same "latest version number"
        // (producing a duplicate version number) or interleave a prune
        // between another upload's add. StorageFile's own methods are
        // synchronized on the same instance, so this block composes with
        // them atomically (reentrant monitor) rather than around them.
        synchronized (file) {
            int nextVersion = file.getLatestVersion().getVersionNumber() + 1;
            file.addVersion(new FileVersion(nextVersion, content, uploaderId));
            file.setVersions(retentionStrategy.apply(file.getVersions()));
        }
        return file;
    }

    @Override
    public String downloadFile(String fileId, String requesterId) throws PermissionDeniedException {
        StorageFile file = requireFile(fileId);
        if (!file.hasAtLeast(requesterId, PermissionLevel.VIEWER)) {
            throw new PermissionDeniedException(requesterId + " does not have VIEWER access to " + fileId);
        }
        return file.getLatestVersion().getContent();
    }

    @Override
    public void shareItem(String itemId, String granterId, String targetUserId, PermissionLevel level) throws PermissionDeniedException {
        FileSystemItem item = requireItem(itemId);
        if (!item.hasAtLeast(granterId, PermissionLevel.OWNER)) {
            throw new PermissionDeniedException(granterId + " does not have OWNER access to " + itemId + ", cannot share it");
        }
        item.grantPermission(targetUserId, level);
    }

    @Override
    public List<FileSystemItem> listChildren(String folderId, String requesterId) throws PermissionDeniedException {
        Folder folder = requireFolder(folderId);
        if (!folder.hasAtLeast(requesterId, PermissionLevel.VIEWER)) {
            throw new PermissionDeniedException(requesterId + " does not have VIEWER access to " + folderId);
        }
        List<FileSystemItem> children = new ArrayList<>();
        for (String childId : folder.getChildIds()) {
            FileSystemItem child = items.get(childId);
            if (child != null) children.add(child);
        }
        return children;
    }

    private void linkToParent(FileSystemItem item, String parentFolderId) {
        if (parentFolderId == null) return;
        Folder parent = requireFolder(parentFolderId);
        parent.addChild(item.getId());
    }

    private void requireEditAccess(String parentFolderId, String userId) throws PermissionDeniedException {
        if (parentFolderId == null) return; // uploading to the root is always allowed
        Folder parent = requireFolder(parentFolderId);
        if (!parent.hasAtLeast(userId, PermissionLevel.EDITOR)) {
            throw new PermissionDeniedException(userId + " does not have EDITOR access to folder " + parentFolderId);
        }
    }

    private Folder requireFolder(String folderId) {
        FileSystemItem item = items.get(folderId);
        if (!(item instanceof Folder)) throw new IllegalArgumentException("Unknown folder: " + folderId);
        return (Folder) item;
    }

    private StorageFile requireFile(String fileId) {
        FileSystemItem item = items.get(fileId);
        if (!(item instanceof StorageFile)) throw new IllegalArgumentException("Unknown file: " + fileId);
        return (StorageFile) item;
    }

    private FileSystemItem requireItem(String itemId) {
        FileSystemItem item = items.get(itemId);
        if (item == null) throw new IllegalArgumentException("Unknown item: " + itemId);
        return item;
    }
}
