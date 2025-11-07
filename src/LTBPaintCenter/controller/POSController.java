package LTBPaintCenter.controller;

import LTBPaintCenter.dao.ProductDAO;
import LTBPaintCenter.model.*;
import LTBPaintCenter.view.POSPanel;
import javax.swing.*;
import java.util.List;

/**
 * This controller manages the Point of Sale (POS) system.
 * It handles the checkout process, updates inventory when items are sold,
 * and refreshes the product display.
 */
public class POSController {
    
    private final Inventory inventory;
    private final Report report;
    private final POSPanel view;

    /**
     * Constructor - sets up the POS controller and view.
     * 
     * @param inventory The inventory system
     * @param report The sales report system
     */
    public POSController(Inventory inventory, Report report) {
        this.inventory = inventory;
        this.report = report;
        this.view = new POSPanel();

        attachHandlers();
        refreshPOS();
    }

    /**
     * Attaches event handlers to the view.
     */
    private void attachHandlers() {
        view.setCheckoutHandler(this::handleCheckout);
    }

    /**
     * Handles the checkout process when a sale is completed.
     * Validates stock, updates inventory, and records the sale.
     * 
     * @param cart The list of items being purchased
     * @return true if checkout was successful, false otherwise
     */
    private boolean handleCheckout(List<SaleItem> cart) {
        if (cart == null || cart.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Cart is empty");
            return false;
        }

        // Show checkout confirmation dialog with summary
        java.awt.Frame owner = null;
        java.awt.Window window = SwingUtilities.getWindowAncestor(view);
        if (window instanceof java.awt.Frame frame) {
            owner = frame;
        }
        
        LTBPaintCenter.view.CheckoutDialog dialog = 
                new LTBPaintCenter.view.CheckoutDialog(owner, cart);
        dialog.setVisible(true);
        
        if (!dialog.isConfirmed()) {
            return false;  // User cancelled
        }

        // Generate a sale ID
        String saleId = "S" + (report.getSales().size() + 1);
        Sale sale = new Sale(saleId);

        try {
            // Process each item in the cart
            for (SaleItem item : cart) {
                Product product = inventory.getProduct(item.getProductId());
                
                // Validate product exists
                if (product == null) {
                    throw new Exception("Product not found: " + item.getName());
                }
                
                // Validate sufficient stock
                if (item.getQty() > product.getQuantity()) {
                    throw new Exception("Not enough stock for " + product.getName());
                }

                // Add item to sale
                sale.addItem(item);
                
                // Update inventory (reduce quantity)
                inventory.updateQuantity(item.getProductId(), -item.getQty());
            }

            // Record the sale
            report.recordSale(sale);
            refreshPOS();
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(view, "Checkout failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Refreshes the POS display with the latest products.
     * Loads products directly from the database to show current stock levels.
     */
    public void refreshPOS() {
        // Get all available products from database
        java.util.List<Product> products = ProductDAO.getAvailableForPOS();
        java.util.List<ProductBatch> batches = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        
        // Convert Products to ProductBatches for the POS view
        for (Product product : products) {
            // Only include products that are in stock and not expired
            if (product.getQuantity() > 0 && 
                (product.getExpirationDate() == null || 
                 product.getExpirationDate().isAfter(today))) {
                batches.add(new ProductBatch(
                        product.getId(), 
                        product.getName(), 
                        product.getBrand(), 
                        product.getColor(), 
                        product.getType(),
                        product.getPrice(), 
                        product.getQuantity(), 
                        product.getDateImported(), 
                        product.getExpirationDate()
                ));
            }
        }
        
        view.refreshProducts(batches);
    }

    /**
     * Gets the POS view panel.
     * 
     * @return The POSPanel view
     */
    public POSPanel getView() {
        return view;
    }
}
