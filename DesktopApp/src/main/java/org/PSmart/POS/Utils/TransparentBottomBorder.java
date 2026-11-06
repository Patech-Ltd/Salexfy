package org.PSmart.POS.Utils;

import javax.swing.border.Border;
import java.awt.*;

public class TransparentBottomBorder implements Border {

    private Color color;
        public TransparentBottomBorder(Color color)
        {
            super();
            this.color=color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setColor(color);  // Set the color and transparency
            g2.drawLine(x, y + height - 1, x + width - 1, y + height - 1);

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 2, 0);  // Adjust the bottom margin as needed
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }