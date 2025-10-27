package LTBPaintCenter.model;

import java.util.*;

public class Inventory {
    private final Map<Integer, Product> map = new HashMap<>();

    public Product getProduct(int id) {
        return map.get(id);
    }

    public void removeProduct(int id) {
        map.remove(id);
    }

    public void addProduct(Product p) {
        map.put(p.getId(), p);
    }

    public Collection<Product> getAll() {
        return map.values();
    }

    /**
     * Adapter: returns current inventory as ProductBatch objects for views that operate on batches.
     * Note: since legacy inventory stores aggregated Product without explicit status, status is
     * computed by ProductBatch based on expirationDate.
     */
    public Collection<ProductBatch> getAllBatches() {
        List<ProductBatch> list = new ArrayList<>();
        for (Product p : map.values()) {
            ProductBatch b = new ProductBatch(
                    p.getId(),
                    p.getName(),
                    p.getBrand(),
                    p.getColor(),
                    p.getType(),
                    p.getPrice(),
                    p.getQuantity(),
                    p.getDateImported(),
                    p.getExpirationDate()
            );
            if (p.getStatus() != null) {
                b.setStatus(p.getStatus());
            }
            list.add(b);
        }
        return list;
    }

    public void updateQuantity(int id, int delta) {
        Product p = map.get(id);
        if (p != null) {
            p.setQuantity(Math.max(0, p.getQuantity() + delta));
        }
    }

    public void clear() {
        map.clear();
    }
}