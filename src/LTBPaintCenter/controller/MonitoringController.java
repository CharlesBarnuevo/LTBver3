package LTBPaintCenter.controller;

import LTBPaintCenter.dao.ProductDAO;
import LTBPaintCenter.dao.TransactionsDAO;
import LTBPaintCenter.model.*;
import LTBPaintCenter.view.MonitoringPanel;

import javax.swing.*;
import java.text.SimpleDateFormat;
import java.util.*;

    //MonitoringController, handles sales filtering, summary updates, and revenue chart visualization.
public class MonitoringController {
    private final Report report;
    private final Inventory inventory;
    private final MonitoringPanel view;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

    private final Map<String, Double> brandTotals = new LinkedHashMap<>();
    private final Map<String, Double> typeTotals = new LinkedHashMap<>();

    public MonitoringController(Report report, Inventory inventory) {
        this.report = report;
        this.inventory = inventory;
        this.view = new MonitoringPanel();
        attachListeners();
        // Wire alert click to navigate to Inventory and select the product
        this.view.setOnAlertClick(productId -> {
            SwingUtilities.invokeLater(() -> {
                java.awt.Window wnd = SwingUtilities.getWindowAncestor(view);
                if (wnd instanceof LTBPaintCenter.view.MainFrame mf) {
                    mf.showPanel("Inventory");
                    if (LTBPaintCenter.model.Global.inventoryController != null) {
                        javax.swing.JPanel invView = LTBPaintCenter.model.Global.inventoryController.getView();
                        if (invView instanceof LTBPaintCenter.view.InventoryPanel ip) {
                            ip.selectRowById(productId, true);
                        }
                    }
                }
            });
        });
        populateBrandFilter();
        refresh();
    }

    public JPanel getView() {
        return view;
    }

    private void attachListeners() {
        view.getBtnApplyFilter().addActionListener(e -> applyFilters());
        view.getBtnClearFilter().addActionListener(e -> clearFilters());

        view.getCbChartMode().addActionListener(e -> {
            String selected = (String) view.getCbChartMode().getSelectedItem();
            if ("Type Revenue".equals(selected)) {
                view.getBarChartPanel().setData(typeTotals);
            } else {
                view.getBarChartPanel().setData(brandTotals);
            }
        });
    }

        public void refresh() {
            List<Sale> allSales = report.getSales();
            view.refreshSales(allSales);

            updateBreakdownSummaries(allSales);

            populateBrandFilter();

            // Update alerts with latest inventory data from DB (not the in-memory cache)
            java.util.List<InventoryBatch> batches = new java.util.ArrayList<>();
            for (Product p : ProductDAO.getAll()) {
                InventoryBatch ib = new InventoryBatch(
                        p.getId(), p.getName(), p.getBrand(), p.getColor(), p.getType(),
                        p.getPrice(), p.getQuantity(), p.getDateImported(), p.getExpirationDate(), p.getStatus()
                );
                batches.add(ib);
            }
            view.updateAlerts(batches);
        }

        private void populateBrandFilter() {
        Set<String> brands = new TreeSet<>();
        for (Product p : inventory.getAll()) {
            if (p.getBrand() != null && !p.getBrand().isBlank()) {
                brands.add(p.getBrand());
            }
        }
        view.populateBrandFilter(brands);
    }

    private void applyFilters() {
        String selectedBrand = Objects.requireNonNull(view.getCbFilterBrand().getSelectedItem()).toString();

        Date dateFrom = parseDateFromSelectors(view.getFromDay(), view.getFromMonth(), view.getFromYear());
        Date dateTo = parseDateFromSelectors(view.getToDay(), view.getToMonth(), view.getToYear());

        if (dateFrom != null && dateTo != null && dateFrom.after(dateTo)) {
            JOptionPane.showMessageDialog(view, "Invalid date range: 'From' cannot be after 'To'.");
            return;
        }

        List<Sale> filtered = new ArrayList<>();
        for (Sale s : report.getSales()) {
            boolean withinDate = dateFrom == null || !s.getDate().before(dateFrom);
            if (dateTo != null && s.getDate().after(dateTo)) withinDate = false;

            boolean matchesBrand = true;
            if (!selectedBrand.equals("All Brands")) {
                matchesBrand = s.getItems().stream().anyMatch(it -> {
                    Product p = inventory.getProduct(it.getProductId());
                    return p != null && selectedBrand.equalsIgnoreCase(p.getBrand());
                });
            }

            if (withinDate && matchesBrand) filtered.add(s);
        }

        view.refreshSales(filtered);
        updateBreakdownSummaries(filtered);
    }

    private Date parseDateFromSelectors(String day, String month, String year) {
        if (day.isBlank() || month.isBlank() || year.isBlank()) return null;

        try {
            int d = Integer.parseInt(day);
            int y = Integer.parseInt(year);
            int m = switch (month) {
                case "Jan" -> 0; case "Feb" -> 1; case "Mar" -> 2; case "Apr" -> 3;
                case "May" -> 4; case "Jun" -> 5; case "Jul" -> 6; case "Aug" -> 7;
                case "Sep" -> 8; case "Oct" -> 9; case "Nov" -> 10; case "Dec" -> 11;
                default -> 0;
            };

            Calendar cal = Calendar.getInstance();
            cal.set(y, m, d, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        } catch (Exception e) {
            return null;
        }
    }

        private void clearFilters() {
            // Reset brand filter
            if (view.getCbFilterBrand() != null && view.getCbFilterBrand().getItemCount() > 0) {
                view.getCbFilterBrand().setSelectedIndex(0);
            }

            // Reset "From" date selectors using combo components
            try {
                java.lang.reflect.Field fDay = view.getClass().getDeclaredField("cbFromDay");
                java.lang.reflect.Field fMonth = view.getClass().getDeclaredField("cbFromMonth");
                java.lang.reflect.Field fYear = view.getClass().getDeclaredField("cbFromYear");
                fDay.setAccessible(true);
                fMonth.setAccessible(true);
                fYear.setAccessible(true);
                ((JComboBox<?>) fDay.get(view)).setSelectedIndex(0);
                ((JComboBox<?>) fMonth.get(view)).setSelectedIndex(0);
                ((JComboBox<?>) fYear.get(view)).setSelectedIndex(0);
            } catch (Exception ignored) {}

            // Reset "To" date selectors using combo components
            try {
                java.lang.reflect.Field fDay = view.getClass().getDeclaredField("cbToDay");
                java.lang.reflect.Field fMonth = view.getClass().getDeclaredField("cbToMonth");
                java.lang.reflect.Field fYear = view.getClass().getDeclaredField("cbToYear");
                fDay.setAccessible(true);
                fMonth.setAccessible(true);
                fYear.setAccessible(true);
                ((JComboBox<?>) fDay.get(view)).setSelectedIndex(0);
                ((JComboBox<?>) fMonth.get(view)).setSelectedIndex(0);
                ((JComboBox<?>) fYear.get(view)).setSelectedIndex(0);
            } catch (Exception ignored) {}

            // Reset chart mode
            if (view.getCbChartMode() != null && view.getCbChartMode().getItemCount() > 0) {
                view.getCbChartMode().setSelectedIndex(0);
            }

            refresh();
        }



        private void updateBreakdownSummaries(Collection<Sale> sales) {
        brandTotals.clear();
        typeTotals.clear();

        for (Sale s : sales) {
            for (SaleItem it : s.getItems()) {
                Product p = inventory.getProduct(it.getProductId());
                String brand = (p != null && p.getBrand() != null && !p.getBrand().isBlank()) ? p.getBrand() : "Unknown";
                String type = (p != null && p.getType() != null && !p.getType().isBlank()) ? p.getType() : "Unknown";
                brandTotals.put(brand, brandTotals.getOrDefault(brand, 0.0) + it.getSubtotal());
                typeTotals.put(type, typeTotals.getOrDefault(type, 0.0) + it.getSubtotal());
            }
        }

        StringBuilder brandText = new StringBuilder();
        for (Map.Entry<String, Double> e : brandTotals.entrySet()) {
            brandText.append(String.format("%s – ₱%.2f%n", e.getKey(), e.getValue()));
        }
        if (brandText.length() == 0) brandText.append("No data available");

        StringBuilder typeText = new StringBuilder();
        for (Map.Entry<String, Double> e : typeTotals.entrySet()) {
            typeText.append(String.format("%s – ₱%.2f%n", e.getKey(), e.getValue()));
        }
        if (typeText.length() == 0) typeText.append("No data available");

        view.updateBreakdown(brandText.toString(), typeText.toString());

        String mode = (String) view.getCbChartMode().getSelectedItem();
        if ("Type Revenue".equals(mode)) {
            view.getBarChartPanel().setData(typeTotals);
        } else {
            view.getBarChartPanel().setData(brandTotals);
        }
    }
}