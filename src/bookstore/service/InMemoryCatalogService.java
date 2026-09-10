package bookstore.service;

import bookstore.model.Book;
import bookstore.specification.BookSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryCatalogService implements CatalogService {
    // ConcurrentHashMap over LinkedHashMap: trades away insertion-order
    // iteration (cosmetic, only affects print/listing order) for safe
    // concurrent reads (search/getBook) alongside catalog updates
    // (addBook) -- LinkedHashMap isn't thread-safe under concurrent
    // structural modification.
    private final Map<String, Book> booksByIsbn = new ConcurrentHashMap<>();

    @Override
    public Book addBook(Book book) {
        booksByIsbn.put(book.getIsbn(), book);
        return book;
    }

    @Override
    public Optional<Book> getBook(String isbn) {
        return Optional.ofNullable(booksByIsbn.get(isbn));
    }

    @Override
    public List<Book> search(BookSpecification spec) {
        return booksByIsbn.values().stream().filter(spec::isSatisfiedBy).collect(Collectors.toList());
    }

    @Override
    public List<Book> listAll() {
        return Collections.unmodifiableList(new ArrayList<>(booksByIsbn.values()));
    }
}
