package ecommerce.service;

import ecommerce.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductCatalogService {
    Product addProduct(Product product);
    List<Product> searchByCategory(String category);
    Optional<Product> getProduct(String productId);
}
