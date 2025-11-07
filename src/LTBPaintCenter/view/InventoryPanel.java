package LTBPaintCenter.view;

import LTBPaintCenter.controller.InventoryController;
import LTBPaintCenter.model.InventoryBatch;
import LTBPaintCenter.model.Global;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * This panel provides the Inventory interface.
 * It displays the list of products and its status where you can update and remove it,
 * This section manages the products in the inventory with CRUD.
 */
public class InventoryPanel extends JPanel {
    private final InventoryController controller;
    private final DefaultTableModel tableModel;
    private final JTable table;

    // Product ID field - only visible/editable when updating existing products
    private final JTextField txtCode = new JTextField();
    private final JLabel lblProductId = new JLabel("Product ID:");
    private final JTextField txtName = new JTextField();
    private final JComboBox<String> cbBrand = new JComboBox<>();
    private final JComboBox<String> cbColor = new JComboBox<>();
    private final JComboBox<String> cbType = new JComboBox<>();
    private final JTextField txtPrice = new JTextField();
    private final JTextField txtQty = new JTextField();
    private final JSpinner spDateImported = new JSpinner(new SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
    private final JSpinner spExpiration = new JSpinner(new SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
    private final JCheckBox chkNoExpiration = new JCheckBox("No Expiration");

    // Filters & sorting
    private final JTextField txtSearch = new JTextField();
    private final JComboBox<String> cbFilterBrand = new JComboBox<>();
    private final JComboBox<String> cbFilterColor = new JComboBox<>();
    private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"A–Z (Name)", "Price Low–High", "Price High–Low"});
    private javax.swing.table.TableRowSorter<DefaultTableModel> rowSorter;

    private final JButton btnAdd = new JButton("Add Batch");
    private final JButton btnUpdate = new JButton("Update");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnRefresh = new JButton("Refresh");

    public InventoryPanel(InventoryController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        // Table setup
        String[] columns = {
                "ID", "Product ID", "Name", "Brand", "Color", "Type", "Price", "Qty",
                "Date Imported", "Expiration Date", "Status"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Keep numeric ID in model but hide the view column
        table.removeColumn(table.getColumnModel().getColumn(0));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Populate form fields when selecting a row
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                // No row selected, clear form and disable Product ID field (adding mode)
                clearForm();
                return;
            }
            int row = table.convertRowIndexToModel(viewRow);
            try {
                // Enable Product ID field when updating (row selected)
                txtCode.setEditable(true);
                txtCode.setEnabled(true);
                txtCode.setBackground(Color.WHITE);
                txtCode.setText(String.valueOf(tableModel.getValueAt(row, 1)));
                
                txtName.setText(String.valueOf(tableModel.getValueAt(row, 2)));
                String brand = String.valueOf(tableModel.getValueAt(row, 3));
                String color = String.valueOf(tableModel.getValueAt(row, 4));
                String type = String.valueOf(tableModel.getValueAt(row, 5));
                if (brand != null) cbBrand.setSelectedItem(brand);
                if (color != null) cbColor.setSelectedItem(color);
                if (type != null) cbType.setSelectedItem(type);
                txtPrice.setText(String.valueOf(tableModel.getValueAt(row, 6)));
                txtQty.setText(String.valueOf(tableModel.getValueAt(row, 7)));

                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String impStr = String.valueOf(tableModel.getValueAt(row, 8));
                if (impStr != null && !impStr.isBlank()) {
                    LocalDate ld = LocalDate.parse(impStr, df);
                    java.util.Date d = java.util.Date.from(ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                    spDateImported.setValue(d);
                }

                String expStr = String.valueOf(tableModel.getValueAt(row, 9));
                if (expStr != null && !expStr.isBlank()) {
                    chkNoExpiration.setSelected(false);
                    spExpiration.setEnabled(true);
                    LocalDate ld = LocalDate.parse(expStr, df);
                    java.util.Date d = java.util.Date.from(ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                    spExpiration.setValue(d);
                } else {
                    chkNoExpiration.setSelected(true);
                    spExpiration.setEnabled(false);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Form panel
        JPanel formPanel = new JPanel(new GridLayout(3, 6, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Product Batch Details"));
        formPanel.setBackground(Color.WHITE);

        cbBrand.setEditable(true);
        cbColor.setEditable(true);
        cbType.setEditable(true);

        // Make spinner text fields non-editable (user picks from spinner UI)
        ((JSpinner.DefaultEditor) spDateImported.getEditor()).getTextField().setEditable(false);
        ((JSpinner.DefaultEditor) spExpiration.getEditor()).getTextField().setEditable(false);

        spDateImported.setEditor(new JSpinner.DateEditor(spDateImported, "yyyy-MM-dd"));
        spExpiration.setEditor(new JSpinner.DateEditor(spExpiration, "yyyy-MM-dd"));

        // Row 1: Product ID (disabled when adding, enabled when updating), Name, Brand
        formPanel.add(lblProductId);
        formPanel.add(txtCode);
        formPanel.add(new JLabel("Name:"));
        formPanel.add(txtName);
        formPanel.add(new JLabel("Brand:"));
        formPanel.add(cbBrand);
        
        // Initially disable Product ID field (only enabled when updating)
        txtCode.setEditable(false);
        txtCode.setEnabled(false);
        txtCode.setBackground(new Color(240, 240, 240)); // Visual indicator it's disabled
        txtCode.setText("(Auto-generated when adding)");

        // Row 2: Color, Type, Price
        formPanel.add(new JLabel("Color:"));
        formPanel.add(cbColor);
        formPanel.add(new JLabel("Type:"));
        formPanel.add(cbType);
        formPanel.add(new JLabel("Price:"));
        formPanel.add(txtPrice);

        // Row 3: Quantity, Date Imported, Expiration
        formPanel.add(new JLabel("Quantity:"));
        formPanel.add(txtQty);
        formPanel.add(new JLabel("Date Imported:"));
        formPanel.add(spDateImported);
        formPanel.add(new JLabel("Expiration Date:"));
        JPanel expPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        expPanel.setBackground(Color.WHITE);
        expPanel.add(spExpiration);
        expPanel.add(chkNoExpiration);
        formPanel.add(expPanel);

        chkNoExpiration.addActionListener(e -> spExpiration.setEnabled(!chkNoExpiration.isSelected()));
        spExpiration.setEnabled(!chkNoExpiration.isSelected());

        JPanel northContainer = new JPanel(new BorderLayout(8, 8));
        northContainer.setBackground(Color.WHITE);
        northContainer.add(formPanel, BorderLayout.NORTH);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createTitledBorder("Search, Filter, and Sort"));

        filterBar.add(new JLabel("Search:"));
        txtSearch.setColumns(16);
        filterBar.add(txtSearch);

        filterBar.add(new JLabel("Brand:"));
        cbFilterBrand.addItem("All Brands");
        filterBar.add(cbFilterBrand);

        filterBar.add(new JLabel("Color:"));
        cbFilterColor.addItem("All Colors");
        filterBar.add(cbFilterColor);

        filterBar.add(new JLabel("Sort:"));
        filterBar.add(cbSort);

        northContainer.add(filterBar, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnUpdate);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);
        add(buttonPanel, BorderLayout.SOUTH);

        // Button actions
        btnAdd.addActionListener(e -> handleAdd());
        btnUpdate.addActionListener(e -> handleUpdate());
        btnDelete.addActionListener(e -> handleDelete());
        btnRefresh.addActionListener(e -> refreshTable());

        // Initialize sorting/filtering and row highlighting
        setupFilters();
        refreshTable();
    }

    // Event handlers
    private void handleAdd() {
        // Product ID is auto-generated at save time, no need to read from field

        String name = txtName.getText().trim();
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this, "Product name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocusInWindow();
            return;
        }

        String brand = (cbBrand.getEditor().getItem() != null) ? cbBrand.getEditor().getItem().toString().trim() : "";
        if (brand.isBlank()) {
            JOptionPane.showMessageDialog(this, "Brand is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            cbBrand.requestFocusInWindow();
            return;
        }

        String color = (cbColor.getEditor().getItem() != null) ? cbColor.getEditor().getItem().toString().trim() : "";
        String type = (cbType.getEditor().getItem() != null) ? cbType.getEditor().getItem().toString().trim() : "";

        double price;
        try {
            price = Double.parseDouble(txtPrice.getText().trim());
            if (price < 0) throw new NumberFormatException("negative");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid non-negative price.", "Validation", JOptionPane.WARNING_MESSAGE);
            txtPrice.requestFocusInWindow();
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be at least 1 when adding a batch.", "Validation", JOptionPane.WARNING_MESSAGE);
                txtQty.requestFocusInWindow();
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid integer quantity.", "Validation", JOptionPane.WARNING_MESSAGE);
            txtQty.requestFocusInWindow();
            return;
        }

        java.util.Date impDate = (java.util.Date) spDateImported.getValue();
        LocalDate imported = impDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        LocalDate expiration = null;
        if (!chkNoExpiration.isSelected()) {
            java.util.Date expDate = (java.util.Date) spExpiration.getValue();
            if (expDate == null) {
                JOptionPane.showMessageDialog(this, "Select an expiration date or check 'No Expiration'.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            expiration = expDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if (!expiration.isAfter(imported)) {
                JOptionPane.showMessageDialog(this, "Expiration date must be after Date Imported.", "Validation", JOptionPane.WARNING_MESSAGE);
                spExpiration.requestFocusInWindow();
                return;
            }
        }

        // Delegate to controller - always pass null to force auto-generation for new products
        // This ensures unique IDs even if multiple products are added quickly
        boolean added = controller.addBatch(null, name, brand, color, type, price, qty, imported, expiration);
        if (added) {
            addItemToComboIfMissing(cbBrand, brand);
            addItemToComboIfMissing(cbColor, color);
            addItemToComboIfMissing(cbType, type);

            JOptionPane.showMessageDialog(this, "Batch added successfully!");
            // Clear form and update product ID preview for next entry
            clearForm();
            refreshTable();
            if (Global.posController != null) Global.posController.refreshPOS();
            if (Global.monitoringController != null) Global.monitoringController.refresh();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add batch.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // Clear form fields
    private void clearForm() {
        // Disable Product ID field when adding new products (it will be auto-generated)
        txtCode.setEditable(false);
        txtCode.setEnabled(false);
        txtCode.setBackground(new Color(240, 240, 240));
        txtCode.setText("(Auto-generated when adding)");
        
        txtName.setText("");
        txtPrice.setText("");
        txtQty.setText("");
        cbBrand.setSelectedItem(null);
        cbColor.setSelectedItem(null);
        cbType.setSelectedItem(null);
        spDateImported.setValue(new java.util.Date());
        spExpiration.setValue(new java.util.Date());
        chkNoExpiration.setSelected(false);
        spExpiration.setEnabled(true);
        // Product ID will be auto-generated when adding
    }

    private void handleUpdate() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to update.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);

        int id;
        try {
            id = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 0)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid selection (missing ID).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = txtName.getText().trim();
        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this, "Product name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocusInWindow();
            return;
        }

        String brand = (cbBrand.getEditor().getItem() != null) ? cbBrand.getEditor().getItem().toString().trim() : "";
        if (brand.isBlank()) {
            JOptionPane.showMessageDialog(this, "Brand is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            cbBrand.requestFocusInWindow();
            return;
        }

        String color = (cbColor.getEditor().getItem() != null) ? cbColor.getEditor().getItem().toString().trim() : "";
        String type = (cbType.getEditor().getItem() != null) ? cbType.getEditor().getItem().toString().trim() : "";

        double price;
        try {
            price = Double.parseDouble(txtPrice.getText().trim());
            if (price < 0) throw new NumberFormatException("negative");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid non-negative price.", "Validation", JOptionPane.WARNING_MESSAGE);
            txtPrice.requestFocusInWindow();
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
            if (qty < 0) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
                txtQty.requestFocusInWindow();
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid integer quantity.", "Validation", JOptionPane.WARNING_MESSAGE);
            txtQty.requestFocusInWindow();
            return;
        }

        java.util.Date impDate = (java.util.Date) spDateImported.getValue();
        LocalDate imported = impDate == null ? null : impDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        LocalDate expiration = null;
        if (!chkNoExpiration.isSelected()) {
            java.util.Date expDate = (java.util.Date) spExpiration.getValue();
            if (expDate == null) {
                JOptionPane.showMessageDialog(this, "Select an expiration date or check 'No Expiration'.", "Validation", JOptionPane.WARNING_MESSAGE);
                spExpiration.requestFocusInWindow();
                return;
            }
            expiration = expDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            if (imported != null && !expiration.isAfter(imported)) {
                JOptionPane.showMessageDialog(this, "Expiration date must be after Date Imported.", "Validation", JOptionPane.WARNING_MESSAGE);
                spExpiration.requestFocusInWindow();
                return;
            }
        }

        // Get product code from the form field (visible when updating)
        String productCode = txtCode.getText().trim();
        if (productCode.isBlank()) {
            JOptionPane.showMessageDialog(this, "Product ID is required when updating.", "Validation", JOptionPane.WARNING_MESSAGE);
            txtCode.requestFocusInWindow();
            return;
        }
        
        // Validate that the product code doesn't already exist (unless it's the same product)
        String existingCode = String.valueOf(tableModel.getValueAt(row, 1));
        if (!productCode.equals(existingCode)) {
            // Check if the new product code already exists in the database
            List<InventoryBatch> allBatches = controller.getAllBatches();
            for (InventoryBatch b : allBatches) {
                if (b.getId() != id && productCode.equals(b.getProductCode())) {
                    JOptionPane.showMessageDialog(this, 
                        "Product ID '" + productCode + "' already exists. Please use a unique Product ID.", 
                        "Duplicate Product ID", 
                        JOptionPane.WARNING_MESSAGE);
                    txtCode.requestFocusInWindow();
                    return;
                }
            }
        }
        
        InventoryBatch batch = new InventoryBatch(id, productCode, name, brand, color, type, price, qty, imported, expiration, "");
        boolean updated = controller.updateBatch(batch);
        if (updated) {
            addItemToComboIfMissing(cbBrand, brand);
            addItemToComboIfMissing(cbColor, color);
            addItemToComboIfMissing(cbType, type);

            JOptionPane.showMessageDialog(this, "Batch updated!");
            refreshTable();
            if (Global.posController != null) Global.posController.refreshPOS();
            if (Global.monitoringController != null) Global.monitoringController.refresh();
        } else {
            JOptionPane.showMessageDialog(this, "Update failed.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDelete() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a batch to delete.");
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);

        int id;
        try {
            id = Integer.parseInt(String.valueOf(tableModel.getValueAt(row, 0)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid selection (missing ID).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = controller.deleteBatch(id);
            if (ok) {
                refreshTable();
                if (Global.posController != null) Global.posController.refreshPOS();
                if (Global.monitoringController != null) Global.monitoringController.refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Delete failed.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Refresh table
    public void refreshTable() {
        tableModel.setRowCount(0);
        List<InventoryBatch> batches = controller.getAllBatches();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Populate combos
        java.util.Set<String> brands = new java.util.TreeSet<>();
        java.util.Set<String> colors = new java.util.TreeSet<>();
        java.util.Set<String> types = new java.util.TreeSet<>();

        for (InventoryBatch b : batches) {
            if (b.getBrand() != null && !b.getBrand().isBlank()) brands.add(b.getBrand());
            if (b.getColor() != null && !b.getColor().isBlank()) colors.add(b.getColor());
            if (b.getType() != null && !b.getType().isBlank()) types.add(b.getType());

            String status = computeDisplayStatus(b);
            tableModel.addRow(new Object[]{
                    b.getId(), b.getProductCode(), b.getName(), b.getBrand(), b.getColor(), b.getType(),
                    String.format("%.2f", b.getPrice()), b.getQuantity(),
                    b.getDateImported() != null ? b.getDateImported().format(df) : "",
                    b.getExpirationDate() != null ? b.getExpirationDate().format(df) : "",
                    status
            });
        }

        // Update filter combos
        cbFilterBrand.removeAllItems();
        cbFilterBrand.addItem("All Brands");
        for (String s : brands) cbFilterBrand.addItem(s);

        cbFilterColor.removeAllItems();
        cbFilterColor.addItem("All Colors");
        for (String s : colors) cbFilterColor.addItem(s);

        cbBrand.removeAllItems();
        for (String s : brands) cbBrand.addItem(s);

        cbColor.removeAllItems();
        for (String s : colors) cbColor.addItem(s);

        cbType.removeAllItems();
        for (String s : types) cbType.addItem(s);

        if (rowSorter != null) applyFilters();
    }

    private String computeDisplayStatus(InventoryBatch b) {
        java.time.LocalDate today = java.time.LocalDate.now();
        boolean expired = false;
        boolean expSoon = false;

        if (b.getExpirationDate() != null) {
            java.time.LocalDate exp = b.getExpirationDate();
            if (!exp.isAfter(today)) {
                expired = true; // exp <= today → expired
            } else if (!exp.isAfter(today.plusDays(7))) {
                expSoon = true; // within next 7 days
            }
        }

        boolean out = b.getQuantity() <= 0;
        boolean low = !out && b.getQuantity() <= 5;

        StringBuilder sb = new StringBuilder();
        if (expired) sb.append("Expired");
        else if (expSoon) sb.append("Expiring Soon");

        if (out) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("Out of Stock");
        } else if (low) {
            if (sb.length() > 0) sb.append("; ");
            sb.append("Low Stock");
        }

        return sb.toString();
    }

    private void setupFilters() {
        rowSorter = new javax.swing.table.TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        // Numeric comparator for price (model column 6)
        rowSorter.setComparator(6, (o1, o2) -> {
            try {
                double d1 = Double.parseDouble(o1.toString());
                double d2 = Double.parseDouble(o2.toString());
                return Double.compare(d1, d2);
            } catch (Exception e) { return 0; }
        });

        // Search and filter listeners
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            private void changed() { applyFilters(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { changed(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { changed(); }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        cbFilterBrand.addActionListener(e -> applyFilters());
        cbFilterColor.addActionListener(e -> applyFilters());
        cbSort.addActionListener(e -> applySort());

        // Row renderer to highlight status when not selected; preserve selection highlight when selected
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                java.awt.Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    c.setBackground(tbl.getSelectionBackground());
                    c.setForeground(tbl.getSelectionForeground());
                    return c;
                }
                c.setForeground(Color.BLACK);
                c.setBackground(Color.WHITE);
                int modelRow = tbl.convertRowIndexToModel(row);
                String status = String.valueOf(tableModel.getValueAt(modelRow, 10));
                if (status != null && !status.isBlank()) {
                    Color bg = new Color(255, 235, 205); // default: light peach
                    String s = status.toLowerCase();
                    if (s.contains("expired")) bg = new Color(255, 205, 210); // light red
                    else if (s.contains("expiring")) bg = new Color(255, 224, 178); // light orange
                    else if (s.contains("low stock")) bg = new Color(255, 249, 196); // light yellow
                    else if (s.contains("out of stock")) bg = new Color(224, 224, 224); // light gray
                    c.setBackground(bg);
                }
                return c;
            }
        });

        applySort();
        applyFilters();
    }

    private void applySort() {
        String sort = (String) cbSort.getSelectedItem();
        java.util.List<javax.swing.RowSorter.SortKey> keys = new java.util.ArrayList<>();
        if ("A–Z (Name)".equals(sort)) {
            keys.add(new javax.swing.RowSorter.SortKey(2, javax.swing.SortOrder.ASCENDING));
        } else if ("Price Low–High".equals(sort)) {
            keys.add(new javax.swing.RowSorter.SortKey(6, javax.swing.SortOrder.ASCENDING));
        } else if ("Price High–Low".equals(sort)) {
            keys.add(new javax.swing.RowSorter.SortKey(6, javax.swing.SortOrder.DESCENDING));
        }
        rowSorter.setSortKeys(keys);
    }

    private void applyFilters() {
        String search = txtSearch.getText().trim().toLowerCase();
        String brandSel = (String) cbFilterBrand.getSelectedItem();
        String colorSel = (String) cbFilterColor.getSelectedItem();
        boolean filterBrand = brandSel != null && !brandSel.equals("All Brands");
        boolean filterColor = colorSel != null && !colorSel.equals("All Colors");

        javax.swing.RowFilter<DefaultTableModel, Integer> rf = new javax.swing.RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String code = String.valueOf(entry.getValue(1)).toLowerCase();
                String name = String.valueOf(entry.getValue(2)).toLowerCase();
                String brand = String.valueOf(entry.getValue(3));
                String color = String.valueOf(entry.getValue(4));
                String type = String.valueOf(entry.getValue(5)).toLowerCase();
                boolean matchesSearch = search.isEmpty() || code.contains(search) || name.contains(search) || type.contains(search)
                        || brand.toLowerCase().contains(search) || color.toLowerCase().contains(search);
                boolean matchesBrand = !filterBrand || brand.equals(brandSel);
                boolean matchesColor = !filterColor || color.equals(colorSel);
                return matchesSearch && matchesBrand && matchesColor;
            }
        };
        rowSorter.setRowFilter(rf);
    }

    // Navigate/select a row by its internal numeric ID; optionally focus fields for editing
    public void selectRowById(int id, boolean focusEditor) {
        if (id <= 0) return;
        // Ensure table is up to date
        refreshTable();
        int modelRow = -1;
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            Object v = tableModel.getValueAt(r, 0);
            if (v != null) {
                try {
                    int rid = Integer.parseInt(v.toString());
                    if (rid == id) {
                        modelRow = r;
                        break;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (modelRow == -1) return;
        int viewRow = table.convertRowIndexToView(modelRow);
        if (viewRow < 0) viewRow = modelRow; // fallback if no sorter
        final int rowToSelect = viewRow;
        SwingUtilities.invokeLater(() -> {
            table.getSelectionModel().setSelectionInterval(rowToSelect, rowToSelect);
            table.scrollRectToVisible(table.getCellRect(rowToSelect, 0, true));
            table.requestFocusInWindow();
            if (focusEditor) {
                // Move focus to the name field so user can start editing
                txtName.requestFocusInWindow();
            }
        });
    }

    // Helper: add item to combo only if missing (case-insensitive)
    private void addItemToComboIfMissing(JComboBox<String> combo, String value) {
        if (value == null) return;
        value = value.trim();
        if (value.isEmpty()) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (value.equalsIgnoreCase(combo.getItemAt(i))) return;
        }
        combo.addItem(value);
    }
}
