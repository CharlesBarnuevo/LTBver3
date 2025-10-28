package LTBPaintCenter.util;

import LTBPaintCenter.model.SaleItem;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReceiptPrinter {

    private static final float VAT_RATE = 0.12f;

    public static String generateReceiptText(List<SaleItem> items) {
        return generateReceiptText(items, null);
    }

    public static String generateReceiptText(List<SaleItem> items, String referenceNo) {
        StringBuilder sb = new StringBuilder();
        double subtotal = items.stream().mapToDouble(SaleItem::getSubtotal).sum();
        double vatable = subtotal / (1 + VAT_RATE);
        double vat = subtotal - vatable;
        double total = subtotal;

        sb.append("        LTB Paint Center\n");
        sb.append("      Official Sales Receipt\n");
        sb.append("--------------------------------------\n");
        sb.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        if (referenceNo != null && !referenceNo.isBlank())
            sb.append("Ref No.: ").append(referenceNo).append("\n");
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
        sb.append(String.format("VAT (12%%): %25.2f\n", vat));
        sb.append(String.format("TOTAL: %28.2f\n", total));
        sb.append("--------------------------------------\n");
        sb.append("Thank you for shopping with us!\n");
        sb.append("       - LTB Paint Center -\n");
        return sb.toString();
    }

    public static void saveAsPDF(List<SaleItem> items, String filePath, String referenceNo) {
        try {
            Document doc = new Document(PageSize.A5, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, new FileOutputStream(filePath));
            doc.open();

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL);
            Font boldFont = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);

            Paragraph storeHeader = new Paragraph("LTB Paint Center", headerFont);
            storeHeader.setAlignment(Element.ALIGN_CENTER);
            doc.add(storeHeader);

            Paragraph subHeader = new Paragraph("Official Sales Receipt\n\n", normalFont);
            subHeader.setAlignment(Element.ALIGN_CENTER);
            doc.add(subHeader);

            Paragraph dateInfo = new Paragraph("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), normalFont);
            if (referenceNo != null && !referenceNo.isBlank()) {
                dateInfo.add(new Phrase("\nRef No.: " + referenceNo, normalFont));
            }
            doc.add(dateInfo);

            doc.add(new Paragraph("--------------------------------------------------", normalFont));

            PdfPTable table = new PdfPTable(new float[]{4, 1, 2});
            table.setWidthPercentage(100);
            table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
            table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            PdfPCell h1 = new PdfPCell(new Phrase("Item", boldFont));
            PdfPCell h2 = new PdfPCell(new Phrase("Qty", boldFont));
            PdfPCell h3 = new PdfPCell(new Phrase("Subtotal", boldFont));
            h1.setBorder(Rectangle.BOTTOM);
            h2.setBorder(Rectangle.BOTTOM);
            h3.setBorder(Rectangle.BOTTOM);
            h1.setHorizontalAlignment(Element.ALIGN_LEFT);
            h2.setHorizontalAlignment(Element.ALIGN_CENTER);
            h3.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(h1);
            table.addCell(h2);
            table.addCell(h3);

            double subtotal = 0;
            for (SaleItem item : items) {
                PdfPCell c1 = new PdfPCell(new Phrase(item.getName(), normalFont));
                PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(item.getQty()), normalFont));
                PdfPCell c3 = new PdfPCell(new Phrase(String.format("%.2f", item.getSubtotal()), normalFont));
                c1.setBorder(Rectangle.NO_BORDER);
                c2.setBorder(Rectangle.NO_BORDER);
                c3.setBorder(Rectangle.NO_BORDER);
                c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(c1);
                table.addCell(c2);
                table.addCell(c3);
                subtotal += item.getSubtotal();
            }

            doc.add(table);
            doc.add(new Paragraph("--------------------------------------------------", normalFont));

            double vatable = subtotal / (1 + VAT_RATE);
            double vat = subtotal - vatable;
            double total = subtotal;

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

            doc.add(summary);
            doc.add(new Paragraph("--------------------------------------------------", normalFont));
            Paragraph footer = new Paragraph("Thank you for shopping with us!\n- LTB Paint Center -", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();
            System.out.println("Receipt saved as: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
