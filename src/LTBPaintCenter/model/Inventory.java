package LTBPaintCenter.model;

import java.util.*;

/**
 * This class manages the collection of products in the inventory.
 * It uses a HashMap to store products by their ID for fast lookup.
 * Provides methods to add, remove, get, and update products.
 */
public class Inventory {
    
    // Store products in a map for fast lookup by ID
    private final Map<Integer, Product> map = new HashMap<>();

    /**
     * Gets a product by its ID.
     * 
     * @param id The product ID to look up
     * @return The Product object, or null if not found
     */
    public Product getProduct(int id) {
        return map.get(id);
    }

    /**
     * Removes a product from the inventory.
     * 
     * @param id The ID of the product to remove
     */
    public void removeProduct(int id) {
        map.remove(id);
    }

    /**
     * Adds a product to the inventory.
     * 
     * @param product The Product object to add
     */
    public void addProduct(Product product) {
        map.put(product.getId(), product);
    }

    /**
     * Gets all products in the inventory.
     * 
     * @return A collection of all Product objects
     */
    public Collection<Product> getAll() {
        return map.values();
    }

    /**
     * Converts all products to ProductBatch objects.
     * ProductBatch is used by the POS system.
     * 
     * @return A collection of ProductBatch objects
     */
    public Collection<ProductBatch> getAllBatches() {
        List<ProductBatch> batches = new ArrayList<>();
        
        for (Product product : map.values()) {
            ProductBatch batch = new ProductBatch(
                    product.getId(),
                    product.getName(),
                    product.getBrand(),
                    product.getColor(),
                    product.getType(),
                    product.getPrice(),
                    product.getQuantity(),
                    product.getDateImported(),
                    product.getExpirationDate()
            );
            
            // Copy status if it exists
            if (product.getStatus() != null) {
                batch.setStatus(product.getStatus());
            }
            
            batches.add(batch);
        }
        
        return batches;
    }

    /**
     * Updates the quantity of a product.
     * Adds or subtracts from the current quantity.
     * Quantity will never go below 0.
     * 
     * @param id The product ID
     * @param delta The amount to change (positive to add, negative to subtract)
     */
    public void updateQuantity(int id, int delta) {
        Product product = map.get(id);
        if (product != null) {
            // Make sure quantity doesn't go negative
            int newQuantity = Math.max(0, product.getQuantity() + delta);
            product.setQuantity(newQuantity);
        }
    }

    /**
     * Clears all products from the inventory.
     * Used when reloading from database.
     */
    public void clear() {
        map.clear();
    }
}
