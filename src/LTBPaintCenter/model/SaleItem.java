package LTBPaintCenter.model;

public class SaleItem {
    private final int productId;
    private final String name;
    private final double price;
    private int qty;

    public SaleItem(int productId, String name, double price, int qty) {
        this.productId = productId;
        this.name = name != null ? name : "Unnamed";
        this.price = price;
        this.qty = Math.max(qty, 0);
    }

    public void addQuantity(int additional) {
        this.qty = Math.max(0, this.qty + additional);
    }

    public double getSubtotal() {
        // Round to 2 decimal places for display consistency
        return Math.round(price * qty * 100.0) / 100.0;
    }

    public int getProductId() { return productId; }

    public String getDisplayId() {
        return String.format("P%03d", productId);
    }

    public String getName() { return name; }

    public int getQty() { return qty; }

    public double getPrice() { return price; }

    public void setQty(int qty) {
        this.qty = Math.max(qty, 0);
    }

    @Override
    public String toString() {
        return String.format("%s (x%d) - ₱%.2f", name, qty, getSubtotal());
    }
}
