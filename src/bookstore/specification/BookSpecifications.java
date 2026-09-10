package bookstore.specification;

// Leaf specifications. Each is a one-liner; the interesting behavior is in
// how they compose (see BookSpecification.and/or/not).
public final class BookSpecifications {
    private BookSpecifications() {}

    public static BookSpecification titleContains(String q) {
        String needle = q.toLowerCase();
        return b -> b.getTitle().toLowerCase().contains(needle);
    }

    public static BookSpecification authorContains(String q) {
        String needle = q.toLowerCase();
        return b -> b.getAuthor().toLowerCase().contains(needle);
    }

    public static BookSpecification subjectContains(String q) {
        String needle = q.toLowerCase();
        return b -> b.getSubject().toLowerCase().contains(needle);
    }

    public static BookSpecification priceBelow(double maxPrice) {
        return b -> b.getPrice() < maxPrice;
    }

    public static BookSpecification inStock() {
        return b -> b.getStock() > 0;
    }
}
