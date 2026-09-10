package library.service;

import library.model.Book;
import library.model.BookItem;
import library.model.BookItemStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryCatalogService implements CatalogService {
    private final Map<String, Book> booksByIsbn = new ConcurrentHashMap<>();
    // CopyOnWriteArrayList per ISBN: checkout scans a title's copies far
    // more often (every checkout attempt) than the catalog adds a new
    // physical copy, so optimizing for lock-free, safe-under-iteration
    // reads is the right trade-off here -- the same choice made for
    // ParkingLot's floor list.
    private final Map<String, List<BookItem>> itemsByIsbn = new ConcurrentHashMap<>();
    private final Map<String, BookItem> itemsByBarcode = new ConcurrentHashMap<>();

    @Override
    public Book addBook(Book book) {
        booksByIsbn.put(book.getIsbn(), book);
        itemsByIsbn.putIfAbsent(book.getIsbn(), new CopyOnWriteArrayList<>());
        return book;
    }

    @Override
    public BookItem addBookItem(BookItem item) {
        itemsByIsbn.computeIfAbsent(item.getBook().getIsbn(), k -> new CopyOnWriteArrayList<>()).add(item);
        itemsByBarcode.put(item.getBarcode(), item);
        return item;
    }

    @Override
    public List<Book> searchByTitle(String query) {
        String q = query.toLowerCase();
        return booksByIsbn.values().stream().filter(b -> b.getTitle().toLowerCase().contains(q)).collect(Collectors.toList());
    }

    @Override
    public List<Book> searchByAuthor(String query) {
        String q = query.toLowerCase();
        return booksByIsbn.values().stream().filter(b -> b.getAuthor().toLowerCase().contains(q)).collect(Collectors.toList());
    }

    @Override
    public List<BookItem> getItemsForIsbn(String isbn) {
        return itemsByIsbn.getOrDefault(isbn, List.of());
    }

    @Override
    public Optional<BookItem> findAvailableItem(String isbn) {
        return getItemsForIsbn(isbn).stream().filter(i -> i.getStatus() == BookItemStatus.AVAILABLE).findFirst();
    }

    @Override
    public Optional<BookItem> getItemByBarcode(String barcode) {
        return Optional.ofNullable(itemsByBarcode.get(barcode));
    }
}
