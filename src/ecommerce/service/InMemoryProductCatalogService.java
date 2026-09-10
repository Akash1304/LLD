package ecommerce.service;

import ecommerce.model.Product;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryProductCatalogService implements ProductCatalogService {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    @Override
    public Product addProduct(Product product) {
        products.put(product.getId(), product);
        return product;
    }

    @Override
    public List<Product> searchByCategory(String category) {
        return products.values().stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Product> getProduct(String productId) {
        return Optional.ofNullable(products.get(productId));
    }
}
