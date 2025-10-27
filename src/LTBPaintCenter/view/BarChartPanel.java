package LTBPaintCenter.view;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class BarChartPanel extends JPanel {
    private Map<String, Double> data;
    private double maxValue;

    public BarChartPanel() {
        setPreferredSize(new Dimension(800, 200));
        setBackground(Color.WHITE);
    }

    public void setData(Map<String, Double> data) {
        this.data = data;
        if (data == null || data.isEmpty()) {
            maxValue = 0;
        } else {
            maxValue = data.values().stream().max(Double::compare).orElse(0.0);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (data == null || data.isEmpty()) {
            g.setColor(Color.GRAY);
            g.drawString("No data to display", 20, 30);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int barHeight = 25;
        int x = 100;
        int y = 40;
        int labelWidth = 80;

        FontMetrics fm = g2.getFontMetrics();

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            String label = entry.getKey();
            double value = entry.getValue();

            // Calculate bar length based on max value
            int barLength = (int) ((width - 200) * (value / maxValue));
            g2.setColor(new Color(70, 130, 180)); // blueish tone
            g2.fillRoundRect(x, y - 15, barLength, barHeight, 8, 8);

            // Draw brand name
            g2.setColor(Color.BLACK);
            g2.drawString(label, 20, y);

            // Draw value at the end of the bar
            String valText = String.format("₱%.2f", value);
            g2.drawString(valText, x + barLength + 10, y);

            y += barHeight + 15;
        }
    }
}
