package org.PSmart.POS.ui.palyground;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

public class RoundedTextFieldExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Rounded JTextField Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new FlowLayout());

        JTextField roundedTextField = new JTextField(15);
        roundedTextField.setBorder(new RoundBorder(10));

        panel.add(roundedTextField);

        frame.add(panel);
        frame.setSize(300, 150);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static class RoundBorder extends AbstractBorder {
        private int radius;

        RoundBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(c.getBackground());
            g2.fillRoundRect(x, y, width, height, radius, radius);

            g2.setColor(Color.BLACK);  // Set border color
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius, radius, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = radius;
            return insets;
        }
    }
}
