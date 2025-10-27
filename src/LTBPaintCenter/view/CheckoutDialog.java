package LTBPaintCenter.view;

import LTBPaintCenter.model.SaleItem;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CheckoutDialog extends JDialog {
    private double subtotal;
    private double vatRate = 0.12; // 12% VAT
    private List<SaleItem> cartItems;

    private JLabel lblVatable = new JLabel();
    private JLabel lblNonVat = new JLabel();
    private JLabel lblSubtotal = new JLabel();
    private JLabel lblVAT = new JLabel();
    private JLabel lblTotal = new JLabel();
    private JLabel lblRef = new JLabel();

    private boolean confirmed = false;

    private String referenceNo;

    public CheckoutDialog(Frame owner, List<SaleItem> cartItems) {
        super(owner, "Checkout Summary", true);
        this.cartItems = cartItems;

        this.subtotal = cartItems.stream()
                .mapToDouble(SaleItem::getSubtotal)
                .sum();

        // Simple reference number: yyyymmddHHMMss + random 3 digits
        this.referenceNo = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()) +
                String.format("%03d", new java.util.Random().nextInt(1000));

        initUI();
        updateTotals();
    }

    private void initUI() {
        setSize(420, 360);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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

    private void updateTotals() {
        double vatable = subtotal; // For now, all items are vatable
        double nonVat = 0.0;
        double vat = vatable * vatRate;
        double total = vatable + nonVat + vat;
        lblRef.setText(referenceNo);
        lblVatable.setText(String.format("₱%.2f", vatable));
        lblNonVat.setText(String.format("₱%.2f", nonVat));
        lblSubtotal.setText(String.format("₱%.2f", vatable + nonVat));
        lblVAT.setText(String.format("₱%.2f", vat));
        lblTotal.setText(String.format("₱%.2f", total));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void showReceipt() {
        // Simulate printed receipt in a pop-up window
        JDialog receipt = new JDialog(this, "Receipt", true);
        receipt.setSize(420, 520);
        receipt.setLocationRelativeTo(this);
        receipt.setLayout(new BorderLayout(10, 10));

        JTextArea txtReceipt = new JTextArea();
        txtReceipt.setEditable(false);
        txtReceipt.setFont(new Font("Monospaced", Font.PLAIN, 12));

        StringBuilder sb = new StringBuilder();
        sb.append("        LTB Paint Center\n");
        sb.append("      Official Sales Receipt\n");
        sb.append("--------------------------------------\n");
        sb.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        sb.append("Ref No.: ").append(referenceNo).append("\n");
        sb.append("--------------------------------------\n");
        sb.append(String.format("%-20s %5s %10s\n", "Item", "Qty", "Subtotal"));
        sb.append("--------------------------------------\n");

        for (SaleItem item : cartItems) {
            sb.append(String.format("%-20s %5d %10.2f\n",
                    item.getName().length() > 20 ? item.getName().substring(0, 20) : item.getName(),
                    item.getQty(),
                    item.getSubtotal()));
        }

        sb.append("--------------------------------------\n");
        double vatable = subtotal; // assume all VATable for now
        double nonVat = 0.0;
        double vat = vatable * vatRate;
        double sub = vatable + nonVat;
        double total = sub + vat;
        sb.append(String.format("VATable: %26.2f\n", vatable));
        sb.append(String.format("VAT-Exempt: %23.2f\n", nonVat));
        sb.append(String.format("Subtotal: %26.2f\n", sub));
        sb.append(String.format("VAT (12%%): %25.2f\n", vat));
        sb.append(String.format("TOTAL: %28.2f\n", total));
        sb.append("--------------------------------------\n");
        sb.append("Thank you for shopping with us!\n");
        sb.append("       - LTB Paint Center -\n");

        txtReceipt.setText(sb.toString());
        receipt.add(new JScrollPane(txtReceipt), BorderLayout.CENTER);

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
