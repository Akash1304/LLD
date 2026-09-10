package library.model;

import java.util.Objects;

public class Member {
    private final String id;
    private final String name;

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member)) return false;
        return Objects.equals(id, ((Member) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() { return "Member{" + id + ", " + name + '}'; }
}
