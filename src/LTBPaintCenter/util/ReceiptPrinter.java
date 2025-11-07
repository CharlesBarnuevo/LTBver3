package LTBPaintCenter.util;

import LTBPaintCenter.model.SaleItem;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This utility class handles receipt generation and PDF creation.
 * It can generate both text receipts and PDF receipts with proper formatting.
 */
public class ReceiptPrinter {

    // VAT (Value Added Tax) rate is 12% in the Philippines
    private static final float VAT_RATE = 0.12f;

    /**
     * Generates a text receipt for the given items.
     * 
     * @param items The list of items sold
     * @return A formatted text receipt string
     */
    public static String generateReceiptText(List<SaleItem> items) {
        return generateReceiptText(items, null);
    }

    /**
     * Generates a text receipt for the given items with a reference number.
     * 
     * @param items The list of items sold
     * @param referenceNo The sale reference number
     * @return A formatted text receipt string
     */
    public static String generateReceiptText(List<SaleItem> items, String referenceNo) {
        StringBuilder receipt = new StringBuilder();
        
        // Calculate totals
        double subtotal = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
        double vatable = subtotal / (1 + VAT_RATE);
        double vat = subtotal - vatable;
        double total = subtotal;

        // Build receipt header
        receipt.append("        LTB Paint Center\n");
        receipt.append("      Official Sales Receipt\n");
        receipt.append("--------------------------------------\n");
        receipt.append("Date: ").append(
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        
        if (referenceNo != null && !referenceNo.isBlank()) {
            receipt.append("Ref No.: ").append(referenceNo).append("\n");
        }
        
        receipt.append("--------------------------------------\n");
        receipt.append(String.format("%-20s %5s %10s\n", "Item", "Qty", "Subtotal"));
        receipt.append("--------------------------------------\n");

        // Add each item to the receipt
        for (SaleItem item : items) {
            String itemName = item.getName().length() > 20 ? 
                    item.getName().substring(0, 20) : item.getName();
            receipt.append(String.format("%-20s %5d %10.2f\n",
                    itemName, item.getQty(), item.getSubtotal()));
        }

        // Add totals
        receipt.append("--------------------------------------\n");
        receipt.append(String.format("VATable: %26.2f\n", vatable));
        receipt.append(String.format("VAT (12%%): %25.2f\n", vat));
        receipt.append(String.format("TOTAL: %28.2f\n", total));
        receipt.append("--------------------------------------\n");
        receipt.append("Thank you for shopping with us!\n");
        receipt.append("       - LTB Paint Center -\n");
        
        return receipt.toString();
    }

    /**
     * Saves a receipt as a PDF file.
     * Creates a nicely formatted PDF with all sale information.
     * 
     * @param items The list of items sold
     * @param filePath The path where to save the PDF file
     * @param referenceNo The sale reference number
     */
    public static void saveAsPDF(List<SaleItem> items, String filePath, String referenceNo) {
        try {
            // Create PDF document (A5 size for receipt format)
            Document document = new Document(PageSize.A5, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            // Define fonts
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL);
            Font boldFont = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);

            // Add store header
            Paragraph storeHeader = new Paragraph("LTB Paint Center", headerFont);
            storeHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(storeHeader);

            Paragraph subHeader = new Paragraph("Official Sales Receipt\n\n", normalFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(subHeader);

            // Add date and reference number
            Paragraph dateInfo = new Paragraph(
                    "Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), 
                    normalFont);
            if (referenceNo != null && !referenceNo.isBlank()) {
                dateInfo.add(new Phrase("\nRef No.: " + referenceNo, normalFont));
            }
            document.add(dateInfo);

            document.add(new Paragraph("--------------------------------------------------", normalFont));

            // Create table for items
            PdfPTable table = new PdfPTable(new float[]{4, 1, 2});
            table.setWidthPercentage(100);
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            // Add table headers
            PdfPCell header1 = new PdfPCell(new Phrase("Item", boldFont));
            PdfPCell header2 = new PdfPCell(new Phrase("Qty", boldFont));
            PdfPCell header3 = new PdfPCell(new Phrase("Subtotal", boldFont));
            header1.setBorder(Rectangle.BOTTOM);
            header2.setBorder(Rectangle.BOTTOM);
            header3.setBorder(Rectangle.BOTTOM);
            header1.setHorizontalAlignment(Element.ALIGN_LEFT);
            header2.setHorizontalAlignment(Element.ALIGN_CENTER);
            header3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(header1);
            table.addCell(header2);
            table.addCell(header3);

            // Add items to table
            double subtotal = 0;
            for (SaleItem item : items) {
                PdfPCell cell1 = new PdfPCell(new Phrase(item.getName(), normalFont));
                PdfPCell cell2 = new PdfPCell(new Phrase(String.valueOf(item.getQty()), normalFont));
                PdfPCell cell3 = new PdfPCell(new Phrase(String.format("%.2f", item.getSubtotal()), normalFont));
                cell1.setBorder(Rectangle.NO_BORDER);
                cell2.setBorder(Rectangle.NO_BORDER);
                cell3.setBorder(Rectangle.NO_BORDER);
                cell1.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell1);
                table.addCell(cell2);
                table.addCell(cell3);
                subtotal += item.getSubtotal();
            }

            document.add(table);
            document.add(new Paragraph("--------------------------------------------------", normalFont));

            // Calculate VAT and totals
            double vatable = subtotal / (1 + VAT_RATE);
            double vat = subtotal - vatable;
            double total = subtotal;

            // Add summary table
            PdfPTable summary = new PdfPTable(2);
            summary.setWidthPercentage(100);
            summary.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            summary.addCell(new Phrase("VATable Sales:", normalFont));
            summary.addCell(new Phrase(String.format("%.2f", vatable), normalFont));
            summary.addCell(new Phrase("VAT (12%):", normalFont));
            summary.addCell(new Phrase(String.format("%.2f", vat), normalFont));
            summary.addCell(new Phrase("Total Amount:", boldFont));
            PdfPCell totalCell = new PdfPCell(new Phrase(String.format("%.2f", total), boldFont));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setBorder(Rectangle.NO_BORDER);
            summary.addCell(totalCell);

            document.add(summary);
            document.add(new Paragraph("--------------------------------------------------", normalFont));
            
            // Add footer
            Paragraph footer = new Paragraph(
                    "Thank you for shopping with us!\n- LTB Paint Center -", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            System.out.println("Receipt saved as: " + filePath);

        } catch (Exception e) {
            System.err.println("Error saving PDF receipt: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
