package filestorage.driver;

import filestorage.model.FileSystemItem;
import filestorage.model.Folder;
import filestorage.model.PermissionLevel;
import filestorage.model.StorageFile;
import filestorage.service.FileStorageService;
import filestorage.service.InMemoryFileStorageService;
import filestorage.strategy.KeepLastNVersionsStrategy;

import java.util.List;

public class FileStorageDriver {
    public static void main(String[] args) {
        runDemo();
    }

    public static void runDemo() {
        FileStorageService storage = new InMemoryFileStorageService();
        String alice = "alice";
        String bob = "bob";

        try {
            Folder projects = storage.createFolder(alice, "Projects", null);
            System.out.println("Alice created: " + projects);

            StorageFile doc = storage.uploadFile(alice, "design.md", projects.getId(), "v1 draft");
            System.out.println("Alice uploaded: " + doc);

            System.out.println("\nBob tries to read the file before being shared (should fail):");
            try {
                storage.downloadFile(doc.getId(), bob);
            } catch (FileStorageService.PermissionDeniedException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nAlice shares the folder with Bob as EDITOR:");
            storage.shareItem(projects.getId(), alice, bob, PermissionLevel.EDITOR);
            storage.shareItem(doc.getId(), alice, bob, PermissionLevel.EDITOR);

            System.out.println("Bob reads the file: \"" + storage.downloadFile(doc.getId(), bob) + "\"");

            System.out.println("\nBob uploads 3 new versions (retention policy keeps only the last 2):");
            for (int i = 2; i <= 4; i++) {
                storage.uploadNewVersion(doc.getId(), bob, "v" + i + " content", new KeepLastNVersionsStrategy(2));
            }
            StorageFile updated = (StorageFile) storage.listChildren(projects.getId(), alice).get(0);
            System.out.println("Versions retained: " + updated.getVersions());

            System.out.println("\nBob tries to share the folder with Carol (Bob is only EDITOR, not OWNER):");
            try {
                storage.shareItem(projects.getId(), bob, "carol", PermissionLevel.VIEWER);
            } catch (FileStorageService.PermissionDeniedException e) {
                System.out.println("Expected failure: " + e.getMessage());
            }

            System.out.println("\nFolder contents (as Alice): " + storage.listChildren(projects.getId(), alice));
        } catch (FileStorageService.PermissionDeniedException e) {
            System.out.println("Unexpected failure: " + e.getMessage());
        }
    }
}
