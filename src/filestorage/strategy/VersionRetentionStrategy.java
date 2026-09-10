package filestorage.strategy;

import filestorage.model.FileVersion;

import java.util.List;

public interface VersionRetentionStrategy {
    List<FileVersion> apply(List<FileVersion> versions);
}
