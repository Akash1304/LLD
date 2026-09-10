package filestorage.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Folder extends FileSystemItem {
    // CopyOnWriteArrayList: a folder's children are listed (read) far more
    // often than a new item is uploaded into it (write), and this makes
    // both individual add/remove calls atomic and listChildren's
    // iteration safe without an explicit lock.
    private final List<String> childIds = new CopyOnWriteArrayList<>();

    public Folder(String id, String name, String ownerId, String parentFolderId) {
        super(id, name, ownerId, parentFolderId);
    }

    public List<String> getChildIds() { return childIds; }
    public void addChild(String itemId) { childIds.add(itemId); }
    public void removeChild(String itemId) { childIds.remove(itemId); }

    @Override
    public String getType() { return "FOLDER"; }

    @Override
    public String toString() { return "Folder{" + getId() + ", " + getName() + '}'; }
}
