package LTBPaintCenter.view;

import LTBPaintCenter.model.Product;
import LTBPaintCenter.model.ProductBatch;
import LTBPaintCenter.model.SaleItem;
import LTBPaintCenter.model.AlertManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.*;
import java.util.List;


public class POSPanel extends JPanel {
    private static class ScrollableWrapPanel extends JPanel implements javax.swing.Scrollable {
        private static final int COLS = 4; // fixed 4 items per row
        private static final int HGAP = 12;
        private static final int VGAP = 12;
        private static final int PADDING = 12; // border padding inside the titled border
        public ScrollableWrapPanel(LayoutManager layout) { super(layout); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(visibleRect.height - 16, 50); }
        @Override public boolean getScrollableTracksViewportWidth() { return false; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
        @Override public Dimension getPreferredSize() {
            int n = getComponentCount();
            // Determine card size from first child or default to 222x222
            int cardW = 220, cardH = 220;
            if (n > 0) {
                Dimension d = getComponent(0).getPreferredSize();
                if (d != null) { cardW = d.width; cardH = d.height; }
            }
            int rows = (int) Math.ceil(n / (double) COLS);
            int width = COLS * cardW + (COLS - 1) * HGAP + 2 * PADDING;
            int height = (rows == 0 ? cardH : rows * cardH + (rows - 1) * VGAP) + 2 * PADDING;
            Insets in = getInsets();
            width += in.left + in.right;
            height += in.top + in.bottom;
            return new Dimension(width, height);
        }
    }
    private final JPanel productGrid = new ScrollableWrapPanel(new GridLayout(0, 4, 12, 12));
    private final JScrollPane productScroll = new JScrollPane(productGrid);

    private final DefaultTableModel cartTableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Price", "Qty", "Subtotal"}, 0
    ) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable cartTable = new JTable(cartTableModel);
    private final JLabel lblSubtotalLabel = new JLabel("Subtotal: ₱0.00");
    private final JLabel lblVatable = new JLabel("VATable: ₱0.00");
    private final JLabel lblVatExempt = new JLabel("Non-VAT: ₱0.00");
    private final JLabel lblVAT = new JLabel("VAT (12%): ₱0.00");
    private final JLabel lblTotal = new JLabel("TOTAL: ₱0.00");

    // Cart buttons
    private final JButton btnCheckout = new JButton("Checkout");
    private final JButton btnClear = new JButton("Clear Cart");
    private final JButton btnRemove = new JButton("Remove Selected");

    // Internal state
    private final Map<Integer, SaleItem> cart = new HashMap<>();
    private final Map<Integer, ProductBatch> batchMap = new HashMap<>();

    // Filters
    private final JComboBox<String> cbBrand = new JComboBox<>();
    private final JComboBox<String> cbColor = new JComboBox<>();
    private final JComboBox<String> cbType = new JComboBox<>();
    private final JTextField txtSearch = new JTextField(16);
    private final JComboBox<String> cbSort = new JComboBox<>(new String[]{"Name A–Z", "Price Low–High", "Price High–Low"});
    private boolean suppressFilterEvents = false;

    // Checkout handler
    public interface CheckoutHandler { boolean handleCheckout(List<SaleItem> cartSnapshot); }
    private CheckoutHandler checkoutHandler = null;

    // Expiration awareness
    private final AlertManager alertManager = new AlertManager();

    public POSPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(Color.WHITE);

        initFilterBar();
        initProductArea();
        initCartArea();

        // Ensure totals react to any table changes as an extra safety net
        cartTableModel.addTableModelListener(e -> updateTotal());

        // Wire filter listeners
        cbBrand.addActionListener(e -> { if (!suppressFilterEvents) updateProductGrid(batchMap.values()); });
        cbColor.addActionListener(e -> { if (!suppressFilterEvents) updateProductGrid(batchMap.values()); });
        cbType.addActionListener(e -> { if (!suppressFilterEvents) updateProductGrid(batchMap.values()); });
    }

    private void initFilterBar() {
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filters"));

        cbBrand.addItem("All Brands");
        cbColor.addItem("All Colors");
        cbType.addItem("All Types");

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.gridy = 0;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.WEST;

        // Row 0
        filterPanel.add(new JLabel("Search:"), gc);
        gc.gridx++;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        txtSearch.setToolTipText("Search by name, brand, color, or type");
        filterPanel.add(txtSearch, gc);

        gc.gridx++;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        filterPanel.add(new JLabel("Brand:"), gc);
        gc.gridx++;
        filterPanel.add(cbBrand, gc);

        gc.gridx++;
        filterPanel.add(new JLabel("Color:"), gc);
        gc.gridx++;
        filterPanel.add(cbColor, gc);

        // Row 1
        gc.gridy = 1;
        gc.gridx = 0;
        filterPanel.add(new JLabel("Type:"), gc);
        gc.gridx++;
        filterPanel.add(cbType, gc);

        gc.gridx++;
        filterPanel.add(new JLabel("Sort:"), gc);
        gc.gridx++;
        filterPanel.add(cbSort, gc);

        // filler to push left and allow expansion
        gc.gridx++;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        filterPanel.add(Box.createHorizontalGlue(), gc);

        // Live search
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void refilter() { updateProductGrid(batchMap.values()); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refilter(); }
        });
        cbSort.addActionListener(e -> updateProductGrid(batchMap.values()));

        add(filterPanel, BorderLayout.NORTH);
    }

    private final JPanel gridContainer = new JPanel();

    private void initProductArea() {
        gridContainer.setLayout(new BorderLayout());
        gridContainer.setBackground(Color.WHITE);

        productGrid.setBackground(Color.WHITE);
        productScroll.setViewportView(productGrid);
        productScroll.setBorder(BorderFactory.createTitledBorder("Available Products"));
        productScroll.getVerticalScrollBar().setUnitIncrement(16);
        productScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gridContainer.add(productScroll, BorderLayout.CENTER);
        add(gridContainer, BorderLayout.CENTER);

        // Responsive resizing
        productScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                layoutGridToFitWidth();
            }
        });
    }

    private void layoutGridToFitWidth() {
        // Keep FlowLayout so each product card keeps its fixed preferred size (250x250) and wraps.
        productGrid.revalidate();
        productGrid.repaint();
    }

    private void initCartArea() {
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setPreferredSize(new Dimension(350, 0));
        rightPanel.setBackground(Color.WHITE);

        JLabel lblCart = new JLabel("🛒 Cart");
        lblCart.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(lblCart, BorderLayout.WEST);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel totalsPanel = new JPanel();
        totalsPanel.setBackground(Color.WHITE);
        totalsPanel.setLayout(new BoxLayout(totalsPanel, BoxLayout.Y_AXIS));
        lblSubtotalLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblVAT.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblTotal.setAlignmentX(Component.RIGHT_ALIGNMENT);
        totalsPanel.add(lblSubtotalLabel);
        totalsPanel.add(lblVAT);
        totalsPanel.add(lblTotal);
        top.add(totalsPanel, BorderLayout.EAST);

        JScrollPane scroll = new JScrollPane(cartTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        cartTable.setRowHeight(26);
        cartTable.getTableHeader().setReorderingAllowed(false);

        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(btnRemove);
        buttons.add(btnClear);
        buttons.add(btnCheckout);

        btnCheckout.setBackground(new Color(0, 120, 215));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFocusPainted(false);

        rightPanel.add(top, BorderLayout.NORTH);
        rightPanel.add(scroll, BorderLayout.CENTER);
        rightPanel.add(buttons, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);

        btnClear.addActionListener(e -> {
            if (!cart.isEmpty()) {
                int confirm = JOptionPane.showConfirmDialog(this, "Clear entire cart?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) clearCart();
            }
        });

        btnRemove.addActionListener(e -> removeSelectedCartItem());
        btnCheckout.addActionListener(e -> {
            if (checkoutHandler == null) {
                JOptionPane.showMessageDialog(this, "Checkout handler not set!");
                return;
            }
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Cart is empty!");
                return;
            }
            boolean ok = checkoutHandler.handleCheckout(getCartSnapshot());
            if (ok) clearCart();
        });

        cartTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedCartQty();
            }
        });
    }

    public void setCheckoutHandler(CheckoutHandler handler) { this.checkoutHandler = handler; }
    public List<SaleItem> getCartSnapshot() { return new ArrayList<>(cart.values()); }
    public void clearCart() { cart.clear(); refreshCartTable(); }

    /**
     * Refreshes product grid from batches.
     */
    public void refreshProducts(Collection<ProductBatch> batches) {
        if (batches == null) batches = Collections.emptyList();

        batchMap.clear();
        for (ProductBatch b : batches) {
            batchMap.put(b.getId(), b);
        }

        Set<String> brands = new TreeSet<>();
        Set<String> colors = new TreeSet<>();
        Set<String> types = new TreeSet<>();
        for (ProductBatch b : batches) {
            if (b.getBrand() != null && !b.getBrand().isBlank()) brands.add(b.getBrand());
            if (b.getColor() != null && !b.getColor().isBlank()) colors.add(b.getColor());
            if (b.getType() != null && !b.getType().isBlank())  types.add(b.getType());
        }

        suppressFilterEvents = true;
        try {
            cbBrand.removeAllItems(); cbBrand.addItem("All Brands");
            for (String s : brands) cbBrand.addItem(s);

            cbColor.removeAllItems(); cbColor.addItem("All Colors");
            for (String s : colors) cbColor.addItem(s);

            cbType.removeAllItems(); cbType.addItem("All Types");
            for (String s : types) cbType.addItem(s);
        } finally {
            suppressFilterEvents = false;
        }

        updateProductGrid(batchMap.values());
        layoutGridToFitWidth();
    }

    private void updateProductGrid(Collection<ProductBatch> batches) {
        productGrid.removeAll();

        String selectedBrand = Objects.toString(cbBrand.getSelectedItem(), "All Brands");
        String selectedColor = Objects.toString(cbColor.getSelectedItem(), "All Colors");
        String selectedType  = Objects.toString(cbType.getSelectedItem(), "All Types");
        String q = txtSearch.getText() != null ? txtSearch.getText().trim().toLowerCase() : "";

        java.util.List<ProductBatch> list = new ArrayList<>();
        for (ProductBatch b : batches) {
            // Do not hide expired products here — show everything in the grid so users can see full catalog.
            // Expired items are still blocked from being added to the cart during selection.

            boolean brandOk = selectedBrand.equals("All Brands") || selectedBrand.equals(b.getBrand());
            boolean colorOk = selectedColor.equals("All Colors") || selectedColor.equals(b.getColor());
            boolean typeOk  = selectedType.equals("All Types")  || selectedType.equals(b.getType());

            boolean textOk = q.isEmpty() ||
                    (b.getName() != null && b.getName().toLowerCase().contains(q)) ||
                    (b.getBrand() != null && b.getBrand().toLowerCase().contains(q)) ||
                    (b.getColor() != null && b.getColor().toLowerCase().contains(q)) ||
                    (b.getType() != null && b.getType().toLowerCase().contains(q));

            if (brandOk && colorOk && typeOk && textOk) list.add(b);
        }

        String sortOpt = Objects.toString(cbSort.getSelectedItem(), "Name A–Z");
        switch (sortOpt) {
            case "Price Low–High" -> list.sort(Comparator.comparingDouble(ProductBatch::getPrice));
            case "Price High–Low" -> list.sort(Comparator.comparingDouble(ProductBatch::getPrice).reversed());
            default -> list.sort(Comparator.comparing(ProductBatch::getName, String.CASE_INSENSITIVE_ORDER));
        }

        for (ProductBatch b : list) productGrid.add(createProductCard(b));

        productGrid.revalidate();
        productGrid.repaint();
    }

    private JPanel createProductCard(ProductBatch b) {
        JPanel card = new JPanel(new BorderLayout());
        Dimension cardSize = new Dimension(220, 220);
        card.setPreferredSize(cardSize);
        card.setMinimumSize(cardSize);
        card.setMaximumSize(cardSize);
        card.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        card.setBackground(Color.WHITE);

        JLabel imgLabel = new JLabel("🖌️", SwingConstants.CENTER);
        imgLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        card.add(imgLabel, BorderLayout.CENTER);

        JLabel lblName = new JLabel("<html><center>" + b.getName() + "</center></html>", SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel lblPrice = new JLabel(String.format("₱%.2f", b.getPrice()), SwingConstants.CENTER);
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setBackground(Color.WHITE);
        bottom.add(lblName);
        bottom.add(lblPrice);
        card.add(bottom, BorderLayout.SOUTH);

        // Add warnings
        if (b.isExpiringSoon() || b.getQuantity() <= 5) {
            JLabel warn = new JLabel("⚠️", SwingConstants.RIGHT);
            warn.setToolTipText(b.isExpiringSoon()
                    ? "Expiring soon!"
                    : "Low stock: " + b.getQuantity());
            card.add(warn, BorderLayout.NORTH);
        }

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openQuantityDialogAndAdd(b); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { card.setBackground(new Color(245, 245, 245)); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { card.setBackground(Color.WHITE); }
        });

        return card;
    }

    private void openQuantityDialogAndAdd(ProductBatch b) {
        if (b.isExpired()) {
            JOptionPane.showMessageDialog(this, "This batch is expired and cannot be sold.");
            return;
        }

        int stock = b.getQuantity();
        int alreadyInCart = cart.containsKey(b.getId()) ? cart.get(b.getId()).getQty() : 0;
        int available = stock - alreadyInCart;
        if (available <= 0) {
            JOptionPane.showMessageDialog(this, "Out of stock!");
            return;
        }

        QuantityDialog qd = new QuantityDialog(b.getName(), available);
        Integer qty = qd.showDialog();
        if (qty != null && qty > 0) addToCart(b, qty);
    }

    private void addToCart(ProductBatch b, int qty) {
        SaleItem existing = cart.get(b.getId());
        if (existing != null) existing.addQuantity(qty);
        else cart.put(b.getId(), new SaleItem(b.getId(), b.getName(), b.getPrice(), qty));
        refreshCartTable();
    }

    private void refreshCartTable() {
        cartTableModel.setRowCount(0);
        for (SaleItem it : cart.values()) {
            cartTableModel.addRow(new Object[]{
                    it.getProductId(),
                    it.getName(),
                    String.format("%.2f", it.getPrice()),
                    it.getQty(),
                    String.format("%.2f", it.getSubtotal())
            });
        }
        updateTotal();
    }

    private void removeSelectedCartItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) return;
        int id = Integer.parseInt(cartTableModel.getValueAt(row, 0).toString());
        cart.remove(id);
        refreshCartTable();
    }

    private void editSelectedCartQty() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow < 0) return;
        int id = Integer.parseInt(cartTableModel.getValueAt(selectedRow, 0).toString());

        SaleItem item = cart.get(id);
        if (item == null) return;

        ProductBatch batch = batchMap.get(id);
        int available = (batch != null) ? batch.getQuantity() : item.getQty();

        QuantityDialog qd = new QuantityDialog(item.getName(), available);
        Integer newQty = qd.showDialog();

        if (newQty != null) {
            if (newQty <= 0) cart.remove(id);
            else item.setQty(newQty);
            refreshCartTable();
        }
    }

    private void updateTotal() {
        // Ensure UI updates happen on the EDT for consistent repaint behavior
        Runnable r = () -> {
            // Use BigDecimal for accurate currency math and rounding
            java.math.BigDecimal vatable = cart.values().stream()
                    .map(it -> java.math.BigDecimal.valueOf(it.getSubtotal()))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal nonVat = java.math.BigDecimal.ZERO; // all items VATable by default in current business rules
            java.math.BigDecimal subtotal = vatable.add(nonVat);
            java.math.BigDecimal vat = vatable.multiply(java.math.BigDecimal.valueOf(0.12))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal total = subtotal.add(vat).setScale(2, java.math.RoundingMode.HALF_UP);

            // Update labels
            lblSubtotalLabel.setText(String.format("Subtotal: ₱%.2f", subtotal.doubleValue()));
            lblVAT.setText(String.format("VAT (12%%): ₱%.2f", vat.doubleValue()));
            lblTotal.setText(String.format("TOTAL: ₱%.2f", total.doubleValue()));

            // Debug log to help diagnose user report
            System.out.println("[DEBUG_LOG][POSPanel.updateTotal] subtotal=" + subtotal + ", vat=" + vat + ", total=" + total + ", cartSize=" + cart.size());

            // Ensure the UI reflects changes immediately
            java.awt.Container totalsParent = lblTotal.getParent();
            if (totalsParent != null) {
                totalsParent.revalidate();
                totalsParent.repaint();
            }
            POSPanel.this.revalidate();
            POSPanel.this.repaint();
        };
        if (javax.swing.SwingUtilities.isEventDispatchThread()) r.run();
        else javax.swing.SwingUtilities.invokeLater(r);
    }

    // Quantity dialog reused from before
    private static class QuantityDialog extends JDialog {
        private Integer result = null;
        private final JTextField txtQty;

        public QuantityDialog(String name, int max) {
            setModal(true);
            setTitle("Select Quantity");
            setSize(300, 180);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout(8, 8));
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JLabel lblTitle = new JLabel("<html><center>" + name + "<br>(Max: " + max + ")</center></html>", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
            add(lblTitle, BorderLayout.NORTH);

            JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            JButton btnMinus = new JButton("-");
            JButton btnPlus = new JButton("+");
            txtQty = new JTextField("1", 4);
            txtQty.setHorizontalAlignment(JTextField.CENTER);
            txtQty.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            center.add(btnMinus);
            center.add(txtQty);
            center.add(btnPlus);
            add(center, BorderLayout.CENTER);

            btnMinus.addActionListener(e -> adjust(-1));
            btnPlus.addActionListener(e -> adjust(1));

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnCancel = new JButton("Cancel");
            JButton btnAdd = new JButton("Add");
            bottom.add(btnCancel);
            bottom.add(btnAdd);
            add(bottom, BorderLayout.SOUTH);

            btnCancel.addActionListener(e -> { result = null; dispose(); });
            btnAdd.addActionListener(e -> {
                try {
                    int val = Integer.parseInt(txtQty.getText().trim());
                    if (val < 1 || val > max) throw new Exception();
                    result = val;
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Enter a valid number (1–" + max + ")");
                }
            });
        }

        private void adjust(int delta) {
            try {
                int val = Integer.parseInt(txtQty.getText().trim());
                val += delta;
                if (val < 1) val = 1;
                txtQty.setText(String.valueOf(val));
            } catch (Exception ignored) {
                txtQty.setText("1");
            }
        }

        public Integer showDialog() {
            setVisible(true);
            return result;
        }
    }
}
