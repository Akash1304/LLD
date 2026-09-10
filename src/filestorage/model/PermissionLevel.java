package filestorage.model;

// Ordered by increasing access -- VIEWER < EDITOR < OWNER -- so "does this
// user have at least EDITOR access" is a simple ordinal comparison.
public enum PermissionLevel {
    VIEWER,
    EDITOR,
    OWNER
}
