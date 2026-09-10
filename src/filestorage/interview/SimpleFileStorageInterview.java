package filestorage.interview;

import java.util.*;

// Compact, single-file interview-friendly file storage demo.
// Supports: upload with versioning, download with a permission check, and
// sharing with viewer/editor/owner access levels.
public class SimpleFileStorageInterview {
    enum Permission { VIEWER, EDITOR, OWNER }

    static class FileEntry {
        final String id;
        final List<String> versions = new ArrayList<>();
        final Map<String, Permission> acl = new HashMap<>();
        FileEntry(String id, String owner, String content) {
            this.id = id;
            versions.add(content);
            acl.put(owner, Permission.OWNER);
        }
        boolean hasAtLeast(String user, Permission required) {
            Permission p = acl.get(user);
            return p != null && p.ordinal() >= required.ordinal();
        }
    }

    final Map<String, FileEntry> files = new HashMap<>();
    int counter = 1;

    FileEntry upload(String owner, String content) {
        FileEntry file = new FileEntry("FILE-" + counter++, owner, content);
        files.put(file.id, file);
        return file;
    }

    void uploadNewVersion(String fileId, String uploader, String content) throws Exception {
        FileEntry file = files.get(fileId);
        if (!file.hasAtLeast(uploader, Permission.EDITOR)) throw new Exception(uploader + " lacks EDITOR access");
        file.versions.add(content);
    }

    String download(String fileId, String requester) throws Exception {
        FileEntry file = files.get(fileId);
        if (!file.hasAtLeast(requester, Permission.VIEWER)) throw new Exception(requester + " lacks VIEWER access");
        return file.versions.get(file.versions.size() - 1);
    }

    void share(String fileId, String granter, String target, Permission level) throws Exception {
        FileEntry file = files.get(fileId);
        if (!file.hasAtLeast(granter, Permission.OWNER)) throw new Exception(granter + " lacks OWNER access to share");
        file.acl.put(target, level);
    }

    // simple demo to run in ~10-15 minutes in interview
    public static void runDemo() {
        SimpleFileStorageInterview drive = new SimpleFileStorageInterview();
        System.out.println("== Simple File Storage Interview Demo ==");

        FileEntry doc = drive.upload("alice", "v1 draft");
        System.out.println("Alice uploaded " + doc.id);

        System.out.println("\nBob tries to read before being shared:");
        try {
            drive.download(doc.id, "bob");
        } catch (Exception e) {
            System.out.println("Expected failure: " + e.getMessage());
        }

        try {
            System.out.println("\nAlice shares as EDITOR; Bob uploads a new version:");
            drive.share(doc.id, "alice", "bob", Permission.EDITOR);
            drive.uploadNewVersion(doc.id, "bob", "v2 content");
            System.out.println("Latest content: \"" + drive.download(doc.id, "bob") + "\"");
            System.out.println("Version count: " + doc.versions.size());

            System.out.println("\nBob tries to share with Carol (Bob is only EDITOR, not OWNER):");
            drive.share(doc.id, "bob", "carol", Permission.VIEWER);
        } catch (Exception e) {
            System.out.println("Expected failure: " + e.getMessage());
        }
        System.out.println("== Demo complete ==");
    }

    public static void main(String[] args) {
        runDemo();
    }
}
