package LTBPaintCenter.view;

import LTBPaintCenter.model.InventoryBatch;
import LTBPaintCenter.model.Sale;
import LTBPaintCenter.model.SaleItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

/**
 * Monitoring panel for viewing sales records, applying filters,
 * and visualizing revenue breakdown by brand or type.
 * Also includes stock and expiration alerts.
 */
public class MonitoringPanel extends JPanel {
    // Existing components
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Reference No", "Date", "Item", "Initial Price (₱)", "Quantity", "Total (₱)"}, 0
    ) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final JTable table = new JTable(tableModel) {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            if (getParent() instanceof JViewport vp) {
                return getPreferredSize().width < vp.getWidth();
            }
            return super.getScrollableTracksViewportWidth();
        }
    };
    private final JLabel lblTotalSales = new JLabel("Total Sales: 0");
    private final JLabel lblRevenue = new JLabel("Total Revenue: ₱0.00");

    private final JComboBox<String> cbFilterBrand = new JComboBox<>(new String[]{"All Brands"});
    private final JComboBox<String> cbFromDay = new JComboBox<>();
    private final JComboBox<String> cbFromMonth = new JComboBox<>();
    private final JComboBox<String> cbFromYear = new JComboBox<>();
    private final JComboBox<String> cbToDay = new JComboBox<>();
    private final JComboBox<String> cbToMonth = new JComboBox<>();
    private final JComboBox<String> cbToYear = new JComboBox<>();

    private final JButton btnApplyFilter = new JButton("Apply");
    private final JButton btnClearFilter = new JButton("Clear");
    private final JButton btnResetTransactions = new JButton("Reset Transactions");

    private final JTextArea taBrandSummary = new JTextArea();
    private final JTextArea taTypeSummary = new JTextArea();
    private final JComboBox<String> cbChartMode = new JComboBox<>(new String[]{"Brand Revenue", "Type Revenue"});
    private final BarChartPanel barChartPanel = new BarChartPanel();

    private List<Sale> currentSales;
    private static class RowRef {
        final Sale sale;
        final SaleItem item;
        RowRef(Sale sale, SaleItem item) { this.sale = sale; this.item = item; }
    }
    private final java.util.List<RowRef> currentRows = new java.util.ArrayList<>();

    private final JTextArea taAlerts = new JTextArea();

    public MonitoringPanel() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        initTopFilters();
        initTable();
        initSummaryBar();
        initAlertsPanel(); // Add alerts section
    }

    // FILTER BAR
    private void initTopFilters() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        filterPanel.add(new JLabel("Brand:"));
        filterPanel.add(cbFilterBrand);

        filterPanel.add(new JLabel("Date From:"));
        addDateSelectors(filterPanel, cbFromDay, cbFromMonth, cbFromYear);

        filterPanel.add(new JLabel("To:"));
        addDateSelectors(filterPanel, cbToDay, cbToMonth, cbToYear);

        styleButton(btnApplyFilter, new Color(0, 120, 215), Color.WHITE);
        styleButton(btnClearFilter, new Color(108, 117, 125), Color.WHITE);
        styleButton(btnResetTransactions, new Color(220, 53, 69), Color.WHITE); // Danger red
        btnResetTransactions.setToolTipText("Clear all recorded transactions from the database");

        filterPanel.add(btnApplyFilter);
        filterPanel.add(btnClearFilter);
        filterPanel.add(Box.createHorizontalStrut(12));
        //filterPanel.add(btnResetTransactions);
        add(filterPanel, BorderLayout.NORTH);
    }

    private void styleButton(JButton b, Color bg, Color fg) {
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setPreferredSize(new Dimension(90, 26));
    }

    private void addDateSelectors(JPanel panel, JComboBox<String> cbDay, JComboBox<String> cbMonth, JComboBox<String> cbYear) {
        cbDay.addItem("");
        for (int i = 1; i <= 31; i++) cbDay.addItem(String.valueOf(i));
        cbDay.setPreferredSize(new Dimension(50, 25));
        panel.add(cbDay);

        cbMonth.addItem("");
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (String m : months) cbMonth.addItem(m);
        cbMonth.setPreferredSize(new Dimension(70, 25));
        panel.add(cbMonth);

        cbYear.addItem("");
        for (int y = 2020; y <= 2026; y++) cbYear.addItem(String.valueOf(y));
        cbYear.setPreferredSize(new Dimension(70, 25));
        panel.add(cbYear);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        cbYear.setSelectedItem(String.valueOf(currentYear));
    }

    // SALES TABLE
    private void initTable() {
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 245, 245));
                if (c instanceof JLabel lbl) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }
                return c;
            }
        });

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // Apply center alignment to all columns
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        table.getColumnModel().getColumn(0).setPreferredWidth(110); // Reference No
        table.getColumnModel().getColumn(1).setPreferredWidth(140); // Date
        table.getColumnModel().getColumn(2).setPreferredWidth(240); // Item
        table.getColumnModel().getColumn(3).setPreferredWidth(120); // Initial Price
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Quantity
        table.getColumnModel().getColumn(5).setPreferredWidth(110); // Total

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Recorded Sales"));
        add(scroll, BorderLayout.CENTER);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0 && row < currentRows.size()) {
                    RowRef rr = currentRows.get(row);
                    showSaleDetailsDialog(rr.sale);
                }
            }
        });
    }

    // SUMMARY SECTION
    private void initSummaryBar() {
        JPanel summaryContainer = new JPanel();
        summaryContainer.setLayout(new BoxLayout(summaryContainer, BoxLayout.Y_AXIS));
        summaryContainer.setBackground(Color.WHITE);
        summaryContainer.setBorder(BorderFactory.createTitledBorder("Sales Summary"));

        JPanel topSummary = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 8));
        topSummary.setBackground(Color.WHITE);
        lblTotalSales.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblRevenue.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topSummary.add(lblTotalSales);
        topSummary.add(lblRevenue);
        summaryContainer.add(topSummary);

        JPanel summaries = new JPanel(new GridLayout(1, 2, 8, 8));
        summaries.setBackground(Color.WHITE);
        setupSummaryTextArea(taBrandSummary, "Revenue by Brand", summaries);
        setupSummaryTextArea(taTypeSummary, "Revenue by Type", summaries);
        summaryContainer.add(summaries);

        JPanel chartHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        chartHeader.setBackground(Color.WHITE);
        chartHeader.add(new JLabel("Chart Mode:"));
        chartHeader.add(cbChartMode);

        barChartPanel.setBorder(BorderFactory.createTitledBorder("Visual Breakdown"));

        summaryContainer.add(Box.createVerticalStrut(8));
        summaryContainer.add(chartHeader);
        summaryContainer.add(barChartPanel);

        add(summaryContainer, BorderLayout.SOUTH);
    }

    private void setupSummaryTextArea(JTextArea area, String title, JPanel parent) {
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        area.setBackground(new Color(248, 248, 248));
        area.setBorder(BorderFactory.createTitledBorder(title));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        parent.add(area);
    }

    // ALERTS SECTION
    private java.util.function.IntConsumer onAlertClick;

    private static class AlertItem {
        enum Type { EXPIRED, EXPIRING_SOON, OUT_OF_STOCK, LOW_STOCK, HEALTHY }
        final int productId;
        final String text;
        final Type type;
        AlertItem(int productId, String text, Type type) {
            this.productId = productId;
            this.text = text;
            this.type = type;
        }
        @Override public String toString() { return text; }
    }

    private final DefaultListModel<AlertItem> alertModel = new DefaultListModel<>();
    private final JList<AlertItem> lstAlerts = new JList<>(alertModel);

    private void initAlertsPanel() {
        JPanel alertsContainer = new JPanel(new BorderLayout());
        alertsContainer.setBackground(Color.WHITE);
        alertsContainer.setBorder(BorderFactory.createTitledBorder("Stock & Expiration Alerts"));
        alertsContainer.setPreferredSize(new Dimension(360, 0));

        lstAlerts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstAlerts.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lstAlerts.setBackground(Color.WHITE);
        lstAlerts.setFixedCellHeight(24);
        lstAlerts.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AlertItem ai) {
                    Color bg;
                    Color fg;
                    switch (ai.type) {
                        case EXPIRED -> { bg = new Color(255, 235, 238); fg = new Color(183, 28, 28); }
                        case EXPIRING_SOON -> { bg = new Color(255, 249, 196); fg = new Color(230, 81, 0); }
                        case OUT_OF_STOCK -> { bg = new Color(224, 224, 224); fg = new Color(66, 66, 66); }
                        case LOW_STOCK -> { bg = new Color(255, 243, 224); fg = new Color(230, 74, 25); }
                        default -> { bg = new Color(232, 245, 233); fg = new Color(27, 94, 32); }
                    }
                    if (!isSelected) {
                        c.setBackground(bg);
                        c.setForeground(fg);
                    } else {
                        c.setBackground(new Color(51, 153, 255));
                        c.setForeground(Color.WHITE);
                    }
                    if (c instanceof JComponent jc) {
                        jc.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                        jc.setToolTipText("Click to open this product in Inventory");
                        jc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }
                return c;
            }
        });

        lstAlerts.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int idx = lstAlerts.locationToIndex(e.getPoint());
                if (idx < 0) return;
                lstAlerts.setSelectedIndex(idx); // visual indicator
                AlertItem ai = alertModel.getElementAt(idx);
                if (onAlertClick != null && ai.productId > 0 && ai.type != AlertItem.Type.HEALTHY) {
                    onAlertClick.accept(ai.productId);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(lstAlerts);
        alertsContainer.add(scroll, BorderLayout.CENTER);
        add(alertsContainer, BorderLayout.EAST);
    }

    public void updateAlerts(List<InventoryBatch> batches) {
        alertModel.clear();
        LocalDate today = LocalDate.now();

        for (InventoryBatch b : batches) {
            LocalDate exp = b.getExpirationDate();
            int qty = b.getQuantity();

            if (exp != null) {
                long daysLeft = ChronoUnit.DAYS.between(today, exp);
                if (daysLeft <= 7 && daysLeft > 0) {
                    String text = String.format("%s (%s) — Expiring in %d day%s", b.getName(), nullSafe(b.getBrand()), daysLeft, daysLeft==1?"":"s");
                    alertModel.addElement(new AlertItem(b.getId(), text, AlertItem.Type.EXPIRING_SOON));
                } else if (daysLeft <= 0) {
                    String text = String.format("%s (%s) — EXPIRED", b.getName(), nullSafe(b.getBrand()));
                    alertModel.addElement(new AlertItem(b.getId(), text, AlertItem.Type.EXPIRED));
                }
            }

            if (qty == 0) {
                String text = String.format("%s (%s) — OUT OF STOCK", b.getName(), nullSafe(b.getBrand()));
                alertModel.addElement(new AlertItem(b.getId(), text, AlertItem.Type.OUT_OF_STOCK));
            } else if (qty <= 5) {
                String text = String.format("%s (%s) — Low stock: %d left", b.getName(), nullSafe(b.getBrand()), qty);
                alertModel.addElement(new AlertItem(b.getId(), text, AlertItem.Type.LOW_STOCK));
            }
        }

        // Only shows the healthy message when there are products and none triggered alerts
        if (alertModel.isEmpty() && batches != null && !batches.isEmpty()) {
            alertModel.addElement(new AlertItem(0, "All products are healthy and in stock.", AlertItem.Type.HEALTHY));
        }
    }

    private static String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "Unknown" : s;
    }

    // SALES SUMMARY METHODS
    public void refreshSales(Collection<Sale> sales) {
        tableModel.setRowCount(0);
        currentRows.clear();

        double totalRevenue = 0;
        int totalSales = 0; // number of receipts (distinct reference numbers)
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        currentSales = sales instanceof List ? (List<Sale>) sales : List.copyOf(sales);

        for (Sale s : currentSales) {
            totalSales++;
            totalRevenue += s.getTotal();

            for (SaleItem it : s.getItems()) {
                currentRows.add(new RowRef(s, it));
                tableModel.addRow(new Object[]{
                        s.getId(),
                        fmt.format(s.getDate()),
                        it.getName(),
                        String.format("₱%.2f", it.getPrice()),
                        it.getQty(),
                        String.format("₱%.2f", it.getSubtotal())
                });
            }
        }

        lblTotalSales.setText("Total Sales: " + totalSales);
        lblRevenue.setText(String.format("Total Revenue: ₱%.2f", totalRevenue));
    }

    // GETTERS
    public void populateBrandFilter(Collection<String> brands) {
        cbFilterBrand.removeAllItems();
        cbFilterBrand.addItem("All Brands");
        for (String b : brands) cbFilterBrand.addItem(b);
    }

    public void updateBreakdown(String brandText, String typeText) {
        taBrandSummary.setText(brandText);
        taTypeSummary.setText(typeText);
    }

    public JButton getBtnApplyFilter() { return btnApplyFilter; }
    public JButton getBtnClearFilter() { return btnClearFilter; }
    public JComboBox<String> getCbFilterBrand() { return cbFilterBrand; }
    public BarChartPanel getBarChartPanel() { return barChartPanel; }
    public JComboBox<String> getCbChartMode() { return cbChartMode; }

    // Click wiring for alerts
    public void setOnAlertClick(java.util.function.IntConsumer consumer) {
        this.onAlertClick = consumer;
    }

    // Date selector getters used by MonitoringController
    public String getFromDay() {
        Object v = cbFromDay.getSelectedItem();
        return v == null ? "" : v.toString();
    }
    public String getFromMonth() {
        Object v = cbFromMonth.getSelectedItem();
        return v == null ? "" : v.toString();
    }
    public String getFromYear() {
        Object v = cbFromYear.getSelectedItem();
        return v == null ? "" : v.toString();
    }
    public String getToDay() {
        Object v = cbToDay.getSelectedItem();
        return v == null ? "" : v.toString();
    }
    public String getToMonth() {
        Object v = cbToMonth.getSelectedItem();
        return v == null ? "" : v.toString();
    }
    public String getToYear() {
        Object v = cbToYear.getSelectedItem();
        return v == null ? "" : v.toString();
    }

    private void showSaleDetailsDialog(Sale sale) {
        if (sale == null) return;

        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = owner instanceof java.awt.Frame
                ? new JDialog((java.awt.Frame) owner, "Sale Details", true)
                : new JDialog(owner, "Sale Details", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setLayout(new BorderLayout(8, 8));
        dlg.getContentPane().setBackground(Color.WHITE);
        dlg.getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header: Reference No and Date
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        JLabel lblRef = new JLabel("Reference No: " + sale.getId());
        lblRef.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel lblDate = new JLabel("Date: " + fmt.format(sale.getDate()));
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        header.add(lblRef);
        header.add(lblDate);
        dlg.add(header, BorderLayout.NORTH);

        // Table with items reflecting the main table's new columns (per item)
        String[] cols = {"Item", "Initial Price (₱)", "Quantity", "Subtotal (₱)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tbl = new JTable(model);
        tbl.setRowHeight(26);
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        javax.swing.table.DefaultTableCellRenderer center = new javax.swing.table.DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < tbl.getColumnModel().getColumnCount(); i++) {
            tbl.getColumnModel().getColumn(i).setCellRenderer(center);
        }
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tbl.setFillsViewportHeight(true);

        for (SaleItem it : sale.getItems()) {
            model.addRow(new Object[]{
                    it.getName(),
                    String.format("₱%.2f", it.getPrice()),
                    it.getQty(),
                    String.format("₱%.2f", it.getSubtotal())
            });
        }

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        dlg.add(sp, BorderLayout.CENTER);

        // Footer with total and Close button
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel lblTotal = new JLabel(String.format("Total: ₱%.2f", sale.getTotal()));
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        footer.add(lblTotal, BorderLayout.WEST);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dlg.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(btnClose);
        footer.add(btnPanel, BorderLayout.EAST);

        dlg.add(footer, BorderLayout.SOUTH);

        dlg.setSize(650, 420);
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    // For updating alerts externally
    public JButton getBtnResetTransactions() { return btnResetTransactions; }
    public JTextArea getTaAlerts() { return taAlerts; }
}
