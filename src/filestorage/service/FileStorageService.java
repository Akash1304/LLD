package filestorage.service;

import filestorage.model.FileSystemItem;
import filestorage.model.Folder;
import filestorage.model.PermissionLevel;
import filestorage.model.StorageFile;
import filestorage.strategy.VersionRetentionStrategy;

import java.util.List;

public interface FileStorageService {
    Folder createFolder(String ownerId, String name, String parentFolderId);
    StorageFile uploadFile(String ownerId, String name, String parentFolderId, String content) throws PermissionDeniedException;
    StorageFile uploadNewVersion(String fileId, String uploaderId, String content, VersionRetentionStrategy retentionStrategy) throws PermissionDeniedException;
    String downloadFile(String fileId, String requesterId) throws PermissionDeniedException;
    void shareItem(String itemId, String granterId, String targetUserId, PermissionLevel level) throws PermissionDeniedException;
    List<FileSystemItem> listChildren(String folderId, String requesterId) throws PermissionDeniedException;

    class PermissionDeniedException extends Exception {
        public PermissionDeniedException(String message) { super(message); }
    }
}
