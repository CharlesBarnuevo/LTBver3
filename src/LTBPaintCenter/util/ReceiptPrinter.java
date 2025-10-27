package LTBPaintCenter.util;

import LTBPaintCenter.model.SaleItem;

import javax.print.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.print.*;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
// AWT fonts (for printing)
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.Font;

// iText fonts (for PDF)
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;


import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

/**
 * Utility class for generating and printing receipts.
 */
public class ReceiptPrinter {

    private static final float VAT_RATE = 0.12f;

    /**
     * Generate a formatted receipt text (same layout as the dialog).
     */
    public static String generateReceiptText(List<SaleItem> items) {
        return generateReceiptText(items, null);
    }

    /**
     * Generate a formatted receipt text with optional reference number and VAT breakdown.
     */
    public static String generateReceiptText(List<SaleItem> items, String referenceNo) {
        StringBuilder sb = new StringBuilder();
        double vatable = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
        double nonVat = 0.0; // Placeholder until items carry VAT-exempt flag
        double subtotal = vatable + nonVat;
        double vat = vatable * VAT_RATE;
        double total = subtotal + vat;

        sb.append("        LTB Paint Center\n");
        sb.append("      Official Sales Receipt\n");
        sb.append("--------------------------------------\n");
        sb.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        if (referenceNo != null && !referenceNo.isBlank()) sb.append("Ref No.: ").append(referenceNo).append("\n");
        sb.append("--------------------------------------\n");
        sb.append(String.format("%-20s %5s %10s\n", "Item", "Qty", "Subtotal"));
        sb.append("--------------------------------------\n");

        for (SaleItem item : items) {
            sb.append(String.format("%-20s %5d %10.2f\n",
                    item.getName().length() > 20 ? item.getName().substring(0, 20) : item.getName(),
                    item.getQty(),
                    item.getSubtotal()));
        }

        sb.append("--------------------------------------\n");
        sb.append(String.format("VATable: %26.2f\n", vatable));
        sb.append(String.format("VAT-Exempt: %23.2f\n", nonVat));
        sb.append(String.format("Subtotal: %26.2f\n", subtotal));
        sb.append(String.format("VAT (12%%): %25.2f\n", vat));
        sb.append(String.format("TOTAL: %28.2f\n", total));
        sb.append("--------------------------------------\n");
        sb.append("Thank you for shopping with us!\n");
        sb.append("       - LTB Paint Center -\n");
        return sb.toString();
    }

    /**
     * Print the receipt using the default printer.
     */
    public static void printReceipt(List<SaleItem> items) {
        printReceipt(items, null);
    }

    /**
     * Print the receipt with an optional reference number.
     */
    public static void printReceipt(List<SaleItem> items, String referenceNo) {
        String receiptText = generateReceiptText(items, referenceNo);

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Printable() {
            public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 10));

                int y = 20;
                for (String line : receiptText.split("\n")) {
                    g2.drawString(line, 20, y);
                    y += 12;
                }
                return PAGE_EXISTS;
            }
        });

        boolean doPrint = job.printDialog();
        if (doPrint) {
            try {
                job.print();
            } catch (PrinterException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Export the receipt as a PDF using iText.
     */
    public static void saveAsPDF(List<SaleItem> items, String filePath) {
        saveAsPDF(items, filePath, null);
    }

    /**
     * Export the receipt as a PDF using iText with optional reference number and VAT breakdown.
     */
    public static void saveAsPDF(List<SaleItem> items, String filePath, String referenceNo) {
        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(filePath));
            doc.open();

            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 12, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 10, com.itextpdf.text.Font.NORMAL);

            Paragraph header = new Paragraph("LTB Paint Center\nOfficial Sales Receipt\n\n", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            doc.add(header);

            doc.add(new Paragraph("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont));
            if (referenceNo != null && !referenceNo.isBlank()) {
                doc.add(new Paragraph("Ref No.: " + referenceNo, normalFont));
            }
            doc.add(new Paragraph("--------------------------------------\n", normalFont));

            PdfPTable table = new PdfPTable(new float[]{3, 1, 2});
            table.setWidthPercentage(100);
            table.addCell("Item");
            table.addCell("Qty");
            table.addCell("Subtotal");

            double vatable = 0;
            for (SaleItem item : items) {
                table.addCell(new Phrase(item.getName(), normalFont));
                table.addCell(new Phrase(String.valueOf(item.getQty()), normalFont));
                table.addCell(new Phrase(String.format("%.2f", item.getSubtotal()), normalFont));
                vatable += item.getSubtotal();
            }

            doc.add(table);
            doc.add(new Paragraph("--------------------------------------\n", normalFont));

            double nonVat = 0.0; // placeholder
            double subtotal = vatable + nonVat;
            double vat = vatable * VAT_RATE;
            double total = subtotal + vat;
            doc.add(new Paragraph(String.format("VATable: %.2f\nVAT-Exempt: %.2f\nSubtotal: %.2f\nVAT (12%%): %.2f\nTOTAL: %.2f\n",
                    vatable, nonVat, subtotal, vat, total), normalFont));

            doc.add(new Paragraph("--------------------------------------\nThank you for shopping with us!\n- LTB Paint Center -", normalFont));
            doc.close();

            System.out.println("Receipt saved as: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
