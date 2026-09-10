package filestorage.strategy;

import filestorage.model.FileVersion;

import java.util.ArrayList;
import java.util.List;

public class KeepLastNVersionsStrategy implements VersionRetentionStrategy {
    private final int n;

    public KeepLastNVersionsStrategy(int n) {
        this.n = n;
    }

    @Override
    public List<FileVersion> apply(List<FileVersion> versions) {
        if (versions.size() <= n) return new ArrayList<>(versions);
        return new ArrayList<>(versions.subList(versions.size() - n, versions.size()));
    }
}
