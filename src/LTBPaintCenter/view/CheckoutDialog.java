package LTBPaintCenter.view;

import LTBPaintCenter.model.SaleItem;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This dialog shows a checkout summary before completing a sale.
 * Displays VATable amount, VAT, subtotal, and total.
 * User can confirm or cancel the transaction.
 */
public class CheckoutDialog extends JDialog {

    private final double subtotal;
    private final double vatRate = 0.12;  // 12% VAT rate
    private final List<SaleItem> cartItems;

    // Labels for displaying totals
    private final JLabel lblVatable = new JLabel();
    private final JLabel lblNonVat = new JLabel();
    private final JLabel lblSubtotal = new JLabel();
    private final JLabel lblVAT = new JLabel();
    private final JLabel lblTotal = new JLabel();
    private final JLabel lblRef = new JLabel();

    private boolean confirmed = false;
    private final String referenceNo;

    /**
     * Constructor - creates the checkout dialog with cart items.
     *
     * @param owner       The parent frame
     * @param cartItems   The list of items in the cart
     * @param referenceNo The pre-generated sale reference number (MMDDYYXXX)
     */
    public CheckoutDialog(Frame owner, List<SaleItem> cartItems, String referenceNo) {
        super(owner, "Checkout Summary", true);
        this.cartItems = cartItems;
        this.referenceNo = referenceNo;

        // Calculate subtotal from all items
        this.subtotal = cartItems.stream()
                .mapToDouble(SaleItem::getSubtotal)
                .sum();

        initUI();
        updateTotals();
    }

    /**
     * Initializes the user interface components.
     */
    private void initUI() {
        setSize(420, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Create center panel with totals
        JPanel center = new JPanel(new GridLayout(6, 2, 10, 10));
        center.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        center.add(new JLabel("Reference No.:"));
        center.add(lblRef);
        center.add(new JLabel("VATable Sale:"));
        center.add(lblVatable);
        center.add(new JLabel("VAT-Exempt Sale:"));
        center.add(lblNonVat);
        center.add(new JLabel("Subtotal:"));
        center.add(lblSubtotal);
        center.add(new JLabel("VAT (12%):"));
        center.add(lblVAT);
        center.add(new JLabel("TOTAL:"));
        center.add(lblTotal);
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD));

        add(center, BorderLayout.CENTER);

        // Create bottom panel with buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnCancel = new JButton("Cancel");
        JButton btnConfirm = new JButton("Confirm");
        
        btnCancel.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        btnConfirm.addActionListener(e -> {
            confirmed = true;
            showReceipt();
        });

        bottom.add(btnCancel);
        bottom.add(btnConfirm);
        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Updates all the total labels with calculated values.
     */
    private void updateTotals() {
        double vatable = subtotal / (1 + vatRate);
        double vat = subtotal - vatable;
        double nonVat = 0.0;  // Currently no VAT-exempt items
        double total = subtotal;

        lblRef.setText(referenceNo);
        lblVatable.setText(String.format("₱%.2f", vatable));
        lblNonVat.setText(String.format("₱%.2f", nonVat));
        lblSubtotal.setText(String.format("₱%.2f", subtotal));
        lblVAT.setText(String.format("₱%.2f", vat));
        lblTotal.setText(String.format("₱%.2f", total));
    }

    /**
     * Checks if the user confirmed the checkout.
     * 
     * @return true if confirmed, false if cancelled
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Shows a receipt dialog with the sale details.
     * This appears after the user confirms the checkout.
     */
    private void showReceipt() {
        // Create a dialog to show the receipt
        JDialog receipt = new JDialog(this, "Receipt", true);
        receipt.setSize(420, 520);
        receipt.setLocationRelativeTo(this);
        receipt.setLayout(new BorderLayout(10, 10));

        // Create text area for receipt display
        JTextArea txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        txtReceipt.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Build receipt text
        StringBuilder receiptText = new StringBuilder();
        receiptText.append("        LTB Paint Center\n");
        receiptText.append("      Official Sales Receipt\n");
        receiptText.append("--------------------------------------\n");
        receiptText.append("Date: ").append(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        receiptText.append("Ref No.: ").append(referenceNo).append("\n");
        receiptText.append("--------------------------------------\n");
        receiptText.append(String.format("%-20s %5s %10s\n", "Item", "Qty", "Subtotal"));
        receiptText.append("--------------------------------------\n");

        // Add each item to receipt
        for (SaleItem item : cartItems) {
            String itemName = item.getName().length() > 20 ? 
                    item.getName().substring(0, 20) : item.getName();
            receiptText.append(String.format("%-20s %5d %10.2f\n",
                    itemName, item.getQty(), item.getSubtotal()));
        }

        receiptText.append("--------------------------------------\n");

        // Calculate totals
        double subtotalWithVat = subtotal;
        double vatable = subtotalWithVat / (1 + vatRate);
        double vat = subtotalWithVat - vatable;
        double nonVat = 0.0;
        double total = subtotalWithVat;

        receiptText.append(String.format("VATable: %26.2f\n", vatable));
        receiptText.append(String.format("VAT-Exempt: %23.2f\n", nonVat));
        receiptText.append(String.format("Subtotal: %26.2f\n", subtotalWithVat));
        receiptText.append(String.format("VAT (12%%): %25.2f\n", vat));
        receiptText.append(String.format("TOTAL: %28.2f\n", total));
        receiptText.append("--------------------------------------\n");
        receiptText.append("Thank you for shopping with us!\n");
        receiptText.append("       - LTB Paint Center -\n");

        txtReceipt.setText(receiptText.toString());
        receipt.add(new JScrollPane(txtReceipt), BorderLayout.CENTER);

        // Add close button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> {
            receipt.dispose();
            dispose();
        });
        bottom.add(btnClose);

        receipt.add(bottom, BorderLayout.SOUTH);
        receipt.setVisible(true);
    }
}
