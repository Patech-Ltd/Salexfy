package org.PSmart.POS.ui.palyground;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class MaterialDesignTextFieldExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Material Design JTextField Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new FlowLayout());

        JTextField materialTextField = new JTextField(15);
        applyMaterialDesign(materialTextField, "Username");

        JTextField passwordField = new JPasswordField(15);
        applyMaterialDesign(passwordField, "Password");

        panel.add(materialTextField);
        panel.add(passwordField);

        frame.add(panel);
        frame.setSize(300, 150);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void applyMaterialDesign(JTextField textField, String placeholder) {
        textField.setBorder(new RoundedBorder(20)); // Set rounded border
        textField.setForeground(Color.GRAY);
        textField.setMargin(new Insets(10, 10, 10, 10)); // Add padding
        textField.setText(placeholder);

        // Add focus listener
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });

        // Ensure the JTextField is explicitly set to be focusable
        textField.setFocusable(true);
    }


    static class RoundedBorder extends AbstractBorder {
        private int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(c.getBackground());
            g2.fillRoundRect(x, y, width, height, radius, radius);

            g2.setColor(Color.BLACK); // Set border color
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
