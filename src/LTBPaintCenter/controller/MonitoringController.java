package LTBPaintCenter.controller;

import LTBPaintCenter.dao.ProductDAO;
import LTBPaintCenter.model.*;
import LTBPaintCenter.view.MonitoringPanel;
import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This controller manages the monitoring and reporting system.
 * It handles sales filtering, generates revenue summaries by brand and type,
 * displays charts, and manages alerts for inventory issues.
 */
public class MonitoringController {
    
    private final Report report;
    private final Inventory inventory;
    private final MonitoringPanel view;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // Track revenue totals by brand and type
    private final Map<String, Double> brandTotals = new LinkedHashMap<>();
    private final Map<String, Double> typeTotals = new LinkedHashMap<>();

    /**
     * Constructor - sets up the monitoring controller and view.
     * 
     * @param report The sales report system
     * @param inventory The inventory system
     */
    public MonitoringController(Report report, Inventory inventory) {
        this.report = report;
        this.inventory = inventory;
        this.view = new MonitoringPanel();
        
        attachListeners();
        
        // When user clicks on an alert, navigate to Inventory panel and select that product
        this.view.setOnAlertClick(productId -> {
            SwingUtilities.invokeLater(() -> {
                java.awt.Window window = SwingUtilities.getWindowAncestor(view);
                if (window instanceof LTBPaintCenter.view.MainFrame mainFrame) {
                    mainFrame.showPanel("Inventory");
                    if (Global.inventoryController != null) {
                        javax.swing.JPanel invView = Global.inventoryController.getView();
                        if (invView instanceof LTBPaintCenter.view.InventoryPanel inventoryPanel) {
                            inventoryPanel.selectRowById(productId, true);
                        }
                    }
                }
            });
        });
        
        populateBrandFilter();
        refresh();
    }

    /**
     * Gets the monitoring view panel.
     * 
     * @return The MonitoringPanel view
     */
    public JPanel getView() {
        return view;
    }

    /**
     * Attaches event listeners to the view components.
     */
    private void attachListeners() {
        view.getBtnApplyFilter().addActionListener(e -> applyFilters());
        view.getBtnClearFilter().addActionListener(e -> clearFilters());

        // Update chart when chart mode changes
        view.getCbChartMode().addActionListener(e -> {
            String selected = (String) view.getCbChartMode().getSelectedItem();
            if ("Type Revenue".equals(selected)) {
                view.getBarChartPanel().setData(typeTotals);
            } else {
                view.getBarChartPanel().setData(brandTotals);
            }
        });
    }

    /**
     * Refreshes all monitoring data.
     * Updates sales list, summaries, filters, and alerts.
     */
    public void refresh() {
        List<Sale> allSales = report.getSales();
        view.refreshSales(allSales);

        updateBreakdownSummaries(allSales);
        populateBrandFilter();

        // Update alerts with latest inventory data from database
        java.util.List<InventoryBatch> batches = new java.util.ArrayList<>();
        for (Product product : ProductDAO.getAll()) {
            InventoryBatch batch = new InventoryBatch(
                    product.getId(), 
                    null,  // product code not needed for alerts
                    product.getName(), 
                    product.getBrand(), 
                    product.getColor(), 
                    product.getType(),
                    product.getPrice(), 
                    product.getQuantity(), 
                    product.getDateImported(), 
                    product.getExpirationDate(), 
                    product.getStatus()
            );
            batches.add(batch);
        }
        view.updateAlerts(batches);
    }

    /**
     * Populates the brand filter dropdown with all available brands.
     */
    private void populateBrandFilter() {
        Set<String> brands = new TreeSet<>();
        for (Product product : inventory.getAll()) {
            if (product.getBrand() != null && !product.getBrand().isBlank()) {
                brands.add(product.getBrand());
            }
        }
        view.populateBrandFilter(brands);
    }

    /**
     * Applies filters to the sales list based on brand and date range.
     */
    private void applyFilters() {
        String selectedBrand = Objects.requireNonNull(
                view.getCbFilterBrand().getSelectedItem()).toString();

        Date dateFrom = parseDateFromSelectors(
                view.getFromDay(), view.getFromMonth(), view.getFromYear());
        Date dateTo = parseDateFromSelectors(
                view.getToDay(), view.getToMonth(), view.getToYear());

        // Validate date range
        if (dateFrom != null && dateTo != null && dateFrom.after(dateTo)) {
            JOptionPane.showMessageDialog(view, 
                    "Invalid date range: 'From' cannot be after 'To'.");
            return;
        }

        // Filter sales
        List<Sale> filtered = new ArrayList<>();
        for (Sale sale : report.getSales()) {
            // Check if sale is within date range
            boolean withinDate = dateFrom == null || !sale.getDate().before(dateFrom);
            if (dateTo != null && sale.getDate().after(dateTo)) {
                withinDate = false;
            }

            // Check if sale matches brand filter
            boolean matchesBrand = true;
            if (!selectedBrand.equals("All Brands")) {
                matchesBrand = sale.getItems().stream().anyMatch(item -> {
                    Product product = inventory.getProduct(item.getProductId());
                    return product != null && selectedBrand.equalsIgnoreCase(product.getBrand());
                });
            }

            if (withinDate && matchesBrand) {
                filtered.add(sale);
            }
        }

        view.refreshSales(filtered);
        updateBreakdownSummaries(filtered);
    }

    /**
     * Parses a date from day, month, and year selectors.
     * 
     * @param day The day string
     * @param month The month string (e.g., "Jan", "Feb")
     * @param year The year string
     * @return A Date object, or null if parsing fails
     */
    private Date parseDateFromSelectors(String day, String month, String year) {
        if (day.isBlank() || month.isBlank() || year.isBlank()) {
            return null;
        }

        try {
            int dayInt = Integer.parseInt(day);
            int yearInt = Integer.parseInt(year);
            
            // Convert month name to number
            int monthInt = switch (month) {
                case "Jan" -> 0; case "Feb" -> 1; case "Mar" -> 2; case "Apr" -> 3;
                case "May" -> 4; case "Jun" -> 5; case "Jul" -> 6; case "Aug" -> 7;
                case "Sep" -> 8; case "Oct" -> 9; case "Nov" -> 10; case "Dec" -> 11;
                default -> 0;
            };

            Calendar calendar = Calendar.getInstance();
            calendar.set(yearInt, monthInt, dayInt, 0, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clears all filters and resets the view to show all sales.
     */
    private void clearFilters() {
        // Reset brand filter
        if (view.getCbFilterBrand() != null && view.getCbFilterBrand().getItemCount() > 0) {
            view.getCbFilterBrand().setSelectedIndex(0);
        }

        // Reset "From" date selectors using reflection
        try {
            java.lang.reflect.Field fieldDay = view.getClass().getDeclaredField("cbFromDay");
            java.lang.reflect.Field fieldMonth = view.getClass().getDeclaredField("cbFromMonth");
            java.lang.reflect.Field fieldYear = view.getClass().getDeclaredField("cbFromYear");
            fieldDay.setAccessible(true);
            fieldMonth.setAccessible(true);
            fieldYear.setAccessible(true);
            ((JComboBox<?>) fieldDay.get(view)).setSelectedIndex(0);
            ((JComboBox<?>) fieldMonth.get(view)).setSelectedIndex(0);
            ((JComboBox<?>) fieldYear.get(view)).setSelectedIndex(0);
        } catch (Exception ignored) {
            // Ignore reflection errors
        }

        // Reset "To" date selectors using reflection
        try {
            java.lang.reflect.Field fieldDay = view.getClass().getDeclaredField("cbToDay");
            java.lang.reflect.Field fieldMonth = view.getClass().getDeclaredField("cbToMonth");
            java.lang.reflect.Field fieldYear = view.getClass().getDeclaredField("cbToYear");
            fieldDay.setAccessible(true);
            fieldMonth.setAccessible(true);
            fieldYear.setAccessible(true);
            ((JComboBox<?>) fieldDay.get(view)).setSelectedIndex(0);
            ((JComboBox<?>) fieldMonth.get(view)).setSelectedIndex(0);
            ((JComboBox<?>) fieldYear.get(view)).setSelectedIndex(0);
        } catch (Exception ignored) {
            // Ignore reflection errors
        }

        // Reset chart mode
        if (view.getCbChartMode() != null && view.getCbChartMode().getItemCount() > 0) {
            view.getCbChartMode().setSelectedIndex(0);
        }

        refresh();
    }

    /**
     * Updates the revenue breakdown summaries by brand and type.
     * Also updates the chart display.
     * 
     * @param sales The collection of sales to analyze
     */
    private void updateBreakdownSummaries(Collection<Sale> sales) {
        brandTotals.clear();
        typeTotals.clear();

        // Calculate totals for each brand and type
        for (Sale sale : sales) {
            for (SaleItem item : sale.getItems()) {
                Product product = inventory.getProduct(item.getProductId());
                String brand = (product != null && product.getBrand() != null && 
                               !product.getBrand().isBlank()) ? product.getBrand() : "Unknown";
                String type = (product != null && product.getType() != null && 
                              !product.getType().isBlank()) ? product.getType() : "Unknown";
                
                brandTotals.put(brand, brandTotals.getOrDefault(brand, 0.0) + item.getSubtotal());
                typeTotals.put(type, typeTotals.getOrDefault(type, 0.0) + item.getSubtotal());
            }
        }

        // Build brand summary text
        StringBuilder brandText = new StringBuilder();
        for (Map.Entry<String, Double> entry : brandTotals.entrySet()) {
            brandText.append(String.format("%s – ₱%.2f%n", entry.getKey(), entry.getValue()));
        }
        if (brandText.length() == 0) {
            brandText.append("No data available");
        }

        // Build type summary text
        StringBuilder typeText = new StringBuilder();
        for (Map.Entry<String, Double> entry : typeTotals.entrySet()) {
            typeText.append(String.format("%s – ₱%.2f%n", entry.getKey(), entry.getValue()));
        }
        if (typeText.length() == 0) {
            typeText.append("No data available");
        }

        // Update the view
        view.updateBreakdown(brandText.toString(), typeText.toString());

        // Update chart based on selected mode
        String mode = (String) view.getCbChartMode().getSelectedItem();
        if ("Type Revenue".equals(mode)) {
            view.getBarChartPanel().setData(typeTotals);
        } else {
            view.getBarChartPanel().setData(brandTotals);
        }
    }
}
