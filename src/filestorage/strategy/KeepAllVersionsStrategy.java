package filestorage.strategy;

import filestorage.model.FileVersion;

import java.util.ArrayList;
import java.util.List;

public class KeepAllVersionsStrategy implements VersionRetentionStrategy {
    @Override
    public List<FileVersion> apply(List<FileVersion> versions) {
        return new ArrayList<>(versions);
    }
}
