package LTBPaintCenter.controller;

import LTBPaintCenter.dao.ProductDAO;
import LTBPaintCenter.model.*;
import LTBPaintCenter.view.MainFrame;
import LTBPaintCenter.util.ReceiptPrinter;

import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

// Main Controller — central hub connecting POS, Inventory, and Monitoring.
public class MainController {
    private final Inventory inventory;
    private final Report report;
    private MainFrame frame;

    private POSController posController;
    private InventoryController inventoryController;
    private MonitoringController monitoringController;

    public MainController() {
        inventory = new Inventory();
        report = new Report();
        Global.inventory = inventory;
        Global.report = report;

        // Load persisted sales before building views so Monitoring shows them
        try {
            report.loadFromDatabase();
        } catch (Exception ignored) {}

        initializeControllers();
        initializeFrame();

        loadProductsFromDatabase();

        posController.getView().setCheckoutHandler(this::handleCheckout);

        frame.showPanel("POS");
        frame.setVisible(true);
    }

    private void initializeControllers() {
        posController = new POSController(inventory, report);
        inventoryController = new InventoryController();
        monitoringController = new MonitoringController(report, inventory);

        Global.inventoryController = inventoryController;
        Global.posController = posController;
        Global.monitoringController = monitoringController;

        System.out.println("InventoryController view hash: " + inventoryController.getView().hashCode());
    }

    private void initializeFrame() {
        frame = new MainFrame(posController, inventoryController, monitoringController);
        frame.addPanel(posController.getView(), "POS");
        frame.addPanel(inventoryController.getView(), "Inventory");
        frame.addPanel(monitoringController.getView(), "Monitoring");
    }

    private void loadProductsFromDatabase() {
        inventory.clear();
        List<Product> dbProducts = ProductDAO.getAll();
        for (Product p : dbProducts) {
            inventory.addProduct(p);
        }

        inventoryController.refreshInventory();
        // POS view expects batches, provide adapted collection
        posController.getView().refreshProducts(inventory.getAllBatches());
        monitoringController.refresh();
    }

    private boolean handleCheckout(List<SaleItem> cart) {
        if (cart == null || cart.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Cart is empty");
            return false;
        }

        // Show checkout summary dialog with VATable, Non-VAT, Subtotal, VAT (12%), and TOTAL
        java.awt.Frame owner = frame;
        LTBPaintCenter.view.CheckoutDialog dialog = new LTBPaintCenter.view.CheckoutDialog(owner, cart);
        dialog.setVisible(true);
        if (!dialog.isConfirmed()) {
            return false; // User cancelled
        }

        try {
            String referenceNo = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()) +
                    String.format("%03d", new java.util.Random().nextInt(1000));
            Sale sale = new Sale(referenceNo);

            for (SaleItem item : cart) {
                sale.addItem(item);

                Product p = inventory.getProduct(item.getProductId());
                if (p != null) {
                    int newQty = Math.max(0, p.getQuantity() - item.getQty());
                    p.setQuantity(newQty);
                    ProductDAO.update(p);
                }
            }

            report.recordSale(sale);
            monitoringController.refresh();
            inventoryController.refreshInventory();

            loadProductsFromDatabase();

            // Ask user to save a PDF receipt
            int choice = JOptionPane.showConfirmDialog(frame, "Would you like to save the receipt as PDF?", "Save Receipt", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                // Use the same reference number used for the sale
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save Receipt PDF");
                chooser.setSelectedFile(new java.io.File("receipt_" + referenceNo + ".pdf"));
                int res = chooser.showSaveDialog(frame);
                if (res == JFileChooser.APPROVE_OPTION) {
                    java.io.File f = chooser.getSelectedFile();
                    String path = f.getAbsolutePath();
                    if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
                    try {
                        ReceiptPrinter.saveAsPDF(cart, path, referenceNo);
                        JOptionPane.showMessageDialog(frame, "Receipt saved to: " + path);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, "Failed to save PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            JOptionPane.showMessageDialog(frame, "Sale recorded successfully!");
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Checkout failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}