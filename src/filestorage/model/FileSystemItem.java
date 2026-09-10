package filestorage.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Shared base for Folder and StorageFile (a Composite-pattern hierarchy):
// both are nameable, ownable, permissioned nodes that can sit inside a
// folder; only StorageFile carries content/versions.
public abstract class FileSystemItem {
    private final String id;
    private volatile String name;
    private final String ownerId;
    private volatile String parentFolderId;
    // ConcurrentHashMap: grantPermission (write) and hasAtLeast (read) are
    // called from concurrent sharing/access-check operations; each is a
    // single atomic map operation, so no additional synchronized wrapper
    // is needed on top of the map's own thread-safety.
    private final Map<String, PermissionLevel> permissions = new ConcurrentHashMap<>();

    protected FileSystemItem(String id, String name, String ownerId, String parentFolderId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.parentFolderId = parentFolderId;
        this.permissions.put(ownerId, PermissionLevel.OWNER);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwnerId() { return ownerId; }
    public String getParentFolderId() { return parentFolderId; }
    public void setParentFolderId(String parentFolderId) { this.parentFolderId = parentFolderId; }

    public void grantPermission(String userId, PermissionLevel level) { permissions.put(userId, level); }
    public PermissionLevel getPermission(String userId) { return permissions.getOrDefault(userId, null); }

    public boolean hasAtLeast(String userId, PermissionLevel required) {
        PermissionLevel level = permissions.get(userId);
        return level != null && level.ordinal() >= required.ordinal();
    }

    public abstract String getType();
}
