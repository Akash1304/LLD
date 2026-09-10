package bookstore.specification;

import bookstore.model.Book;

// Specification: a composable predicate over Book. Three near-identical
// "search by X" strategy classes couldn't be combined -- searching by
// author AND subject meant a fourth class. Specifications compose with
// and/or/not, so any query the UI can express is built from the same
// handful of leaf specs, and the catalog service only ever sees one
// interface.
@FunctionalInterface
public interface BookSpecification {
    boolean isSatisfiedBy(Book book);

    default BookSpecification and(BookSpecification other) {
        return book -> this.isSatisfiedBy(book) && other.isSatisfiedBy(book);
    }

    default BookSpecification or(BookSpecification other) {
        return book -> this.isSatisfiedBy(book) || other.isSatisfiedBy(book);
    }

    default BookSpecification not() {
        return book -> !this.isSatisfiedBy(book);
    }
}
