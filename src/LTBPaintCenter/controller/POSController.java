package LTBPaintCenter.controller;

import LTBPaintCenter.model.*;
import LTBPaintCenter.view.POSPanel;

import javax.swing.*;
import java.util.List;

    //POSController, manages checkout flow and updates product display.
public class POSController {
    private final Inventory inventory;
    private final Report report;
    private final POSPanel view;

    public POSController(Inventory inventory, Report report) {
        this.inventory = inventory;
        this.report = report;
        this.view = new POSPanel();

        attachHandlers();
        refreshPOS();
    }

    private void attachHandlers() {
        view.setCheckoutHandler(this::handleCheckout);
    }

    private boolean handleCheckout(List<SaleItem> cart) {
        if (cart == null || cart.isEmpty()) {
            JOptionPane.showMessageDialog(view, "Cart is empty");
            return false;
        }

        // Show confirmation dialog with summary (VATable, Non-VAT, Subtotal, VAT, TOTAL)
        java.awt.Frame owner = null;
        java.awt.Window win = SwingUtilities.getWindowAncestor(view);
        if (win instanceof java.awt.Frame f) owner = f;
        LTBPaintCenter.view.CheckoutDialog dialog = new LTBPaintCenter.view.CheckoutDialog(owner, cart);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return false; // user cancelled
        }

        String saleId = "S" + (report.getSales().size() + 1);
        Sale sale = new Sale(saleId);

        try {
            for (SaleItem item : cart) {
                Product product = inventory.getProduct(item.getProductId());
                if (product == null) throw new Exception("Product not found: " + item.getName());
                if (item.getQty() > product.getQuantity())
                    throw new Exception("Not enough stock for " + product.getName());

                sale.addItem(item);
                inventory.updateQuantity(item.getProductId(), -item.getQty());
            }

            report.recordSale(sale);
            // No need to show another dialog; receipt shown in CheckoutDialog
            refreshPOS();
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(view, "Checkout failed: " + e.getMessage());
            return false;
        }
    }

    public void refreshPOS() {
        // Refresh products directly from DB to reflect latest Inventory changes
        java.util.List<LTBPaintCenter.model.Product> products = LTBPaintCenter.model.ProductDAO.getAvailableForPOS();
        java.util.List<LTBPaintCenter.model.ProductBatch> batches = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (LTBPaintCenter.model.Product p : products) {
            if (p.getQuantity() > 0 && (p.getExpirationDate() == null || p.getExpirationDate().isAfter(today))) {
                batches.add(new LTBPaintCenter.model.ProductBatch(
                        p.getId(), p.getName(), p.getBrand(), p.getColor(), p.getType(),
                        p.getPrice(), p.getQuantity(), p.getDateImported(), p.getExpirationDate()
                ));
            }
        }
        view.refreshProducts(batches);
    }

    public POSPanel getView() {
        return view;
    }
}