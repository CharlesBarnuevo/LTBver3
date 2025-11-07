package LTBPaintCenter.controller;

import LTBPaintCenter.dao.ProductDAO;
import LTBPaintCenter.dao.SaleReferenceGenerator;
import LTBPaintCenter.model.*;
import LTBPaintCenter.view.MainFrame;
import LTBPaintCenter.util.ReceiptPrinter;
import javax.swing.*;
import java.time.LocalDate;
import java.util.List;

/**
 * This is the main controller that coordinates all the other controllers.
 * It manages the POS (Point of Sale), Inventory, and Monitoring systems.
 * Acts as the central hub connecting all parts of the application.
 */
public class MainController {
    
    private final Inventory inventory;
    private final Report report;
    private MainFrame frame;

    private POSController posController;
    private InventoryController inventoryController;
    private MonitoringController monitoringController;

    /**
     * Constructor - sets up the entire application.
     * Initializes inventory, reports, controllers, and loads data from database.
     */
    public MainController() {
        // Create the main data models
        inventory = new Inventory();
        report = new Report();
        
        // Store them globally so other parts of the app can access them
        Global.inventory = inventory;
        Global.report = report;

        // Load previous sales from database so monitoring can show them
        try {
            report.loadFromDatabase();
        } catch (Exception ignored) {
            // If there are no previous sales, that's okay
        }

        // Set up all the controllers and the main window
        initializeControllers();
        initializeFrame();
        loadProductsFromDatabase();

        // Connect the checkout handler to the POS panel
        posController.getView().setCheckoutHandler(this::handleCheckout);

        // Show the POS panel by default when the app starts
        frame.showPanel("POS");
        frame.setVisible(true);
    }

    /**
     * Gets the main window frame.
     * 
     * @return The MainFrame window
     */
    public LTBPaintCenter.view.MainFrame getFrame() {
        return frame;
    }

    /**
     * Creates and initializes all the controllers.
     * Also stores them in the Global class for easy access.
     */
    private void initializeControllers() {
        posController = new POSController(inventory, report);
        inventoryController = new InventoryController();
        monitoringController = new MonitoringController(report, inventory);

        // Store controllers globally for access from other parts of the app
        Global.inventoryController = inventoryController;
        Global.posController = posController;
        Global.monitoringController = monitoringController;
    }

    /**
     * Creates the main window and adds all the panels to it.
     */
    private void initializeFrame() {
        frame = new MainFrame(posController, inventoryController, monitoringController);
        frame.addPanel(posController.getView(), "POS");
        frame.addPanel(inventoryController.getView(), "Inventory");
        frame.addPanel(monitoringController.getView(), "Monitoring");
    }

    /**
     * Loads all products from the database into the inventory.
     * Also refreshes the inventory and POS views to show the latest data.
     */
    private void loadProductsFromDatabase() {
        // Clear existing inventory
        inventory.clear();
        
        // Get all products from database
        List<Product> dbProducts = ProductDAO.getAll();
        for (Product product : dbProducts) {
            inventory.addProduct(product);
        }

        // Refresh all views to show the updated data
        inventoryController.refreshInventory();
        posController.getView().refreshProducts(inventory.getAllBatches());
        monitoringController.refresh();
    }

    /**
     * Handles the checkout process when a sale is completed.
     * Creates a sale record, updates inventory, and optionally saves a PDF receipt.
     * 
     * @param cart The list of items being purchased
     * @return true if checkout was successful, false otherwise
     */
    private boolean handleCheckout(List<SaleItem> cart) {
        // Check if cart is empty
        if (cart == null || cart.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Cart is empty");
            return false;
        }

        // Show checkout dialog with summary (VATable, Non-VAT, Subtotal, VAT 12%, Total)
        java.awt.Frame owner = frame;
        String referenceNo = SaleReferenceGenerator.generateSaleReference(LocalDate.now());
        LTBPaintCenter.view.CheckoutDialog dialog = new LTBPaintCenter.view.CheckoutDialog(owner, cart, referenceNo);
        dialog.setVisible(true);
        
        // If user cancelled, don't proceed with checkout
        if (!dialog.isConfirmed()) {
            return false;
        }

        try {
            // Create a new sale with this reference number
            Sale sale = new Sale(referenceNo);

            // Process each item in the cart
            for (SaleItem item : cart) {
                sale.addItem(item);

                // Update inventory - reduce quantity by the amount sold
                Product product = inventory.getProduct(item.getProductId());
                if (product != null) {
                    int newQuantity = Math.max(0, product.getQuantity() - item.getQty());
                    product.setQuantity(newQuantity);
                    ProductDAO.update(product);
                }
            }

            // Record the sale in the report
            report.recordSale(sale);
            
            // Refresh all views to show updated data
            monitoringController.refresh();
            inventoryController.refreshInventory();
            loadProductsFromDatabase();

            // Ask user if they want to save a PDF receipt
            int choice = JOptionPane.showConfirmDialog(frame, 
                    "Would you like to save the receipt as PDF?", 
                    "Save Receipt", 
                    JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                // Let user choose where to save the PDF
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save Receipt PDF");
                chooser.setSelectedFile(new java.io.File("receipt_" + referenceNo + ".pdf"));
                
                int result = chooser.showSaveDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    java.io.File file = chooser.getSelectedFile();
                    String path = file.getAbsolutePath();
                    
                    // Make sure the file ends with .pdf
                    if (!path.toLowerCase().endsWith(".pdf")) {
                        path += ".pdf";
                    }
                    
                    try {
                        ReceiptPrinter.saveAsPDF(cart, path, referenceNo);
                        JOptionPane.showMessageDialog(frame, "Receipt saved to: " + path);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(frame, 
                                "Failed to save PDF: " + ex.getMessage(), 
                                "Error", 
                                JOptionPane.ERROR_MESSAGE);
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
