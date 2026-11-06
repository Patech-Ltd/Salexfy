package org.PSmart.POS.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MyUtils {

    public static JButton createRoundedButton(String text,Color bg) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isArmed()) {
                    g.setColor(Color.lightGray);
                } else {
                    g.setColor(bg);
                }

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 10;  // Adjust the arc value to change the roundness
                int width = getWidth();
                int height = getHeight();

                g2.fill(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, arc, arc));

                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                // Draw an empty border to enhance the appearance
                g.setColor(getForeground());
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 10;  // Adjust the arc value to change the roundness
                int width = getWidth();
                int height = getHeight();

                g2.draw(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, arc, arc));
            }
        };

        // Set the button properties
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);

        return button;
    }

    public static JButton createButton(String text,Color bg) {
        JButton button = new JButton(text) {


            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isArmed()) {
                    g.setColor(Color.lightGray);
                } else {
                    g.setColor(getBackground());
                }

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 0;  // Adjust the arc value to change the roundness
                int width = getWidth();
                int height = getHeight();

                g2.fill(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, arc, arc));

                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                // Draw an empty border to enhance the appearance
                g.setColor(getForeground());
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 10;  // Adjust the arc value to change the roundness
                int width = getWidth();
                int height = getHeight();

                g2.draw(new RoundRectangle2D.Double(0, 0, width - 1, height - 1, arc, arc));
            }
        };

        // Set the button properties
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBackground(bg);

        return button;
    }

    public static Font courierFont(int size)
    {
        return new Font("Courier",Font.BOLD,size);
    }

    public static Font arialFont(int size)
    {
        return new Font("Arial",Font.BOLD,size);
    }

    public static String getCurrentDateFormatted() {
        LocalDateTime now = LocalDateTime.now();

        // Format the date and time
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("E, dd-MMM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        String formattedDate = now.format(dateFormatter);
        String formattedTime = now.format(timeFormatter);

        // Construct the HTML string
        return String.format("<html><center>%s<br>%s</html>", formattedDate, formattedTime);
    }

    public static void drawVerticalLine(JPanel panel, int xPosition) {
        JPanel tmp=new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Color.WHITE);
                g.drawLine(0, 0, 0, panel.getHeight());
            }
        };
        tmp.setLocation(xPosition,0);
        tmp.setSize(10,panel.getPreferredSize().height);
        tmp.setOpaque(false);
        panel.add(tmp);
    }



    public static Double getDouble(String str)
    {
        try{
            return Double.parseDouble(str);
        }catch (Exception es)
        {
            return 0.0;
        }
    }

    public static int getInt(String str)
    {
        try{
            return Integer.parseInt(str);
        }catch (Exception es)
        {
            return 0;
        }
    }

    public static String formatCurrency(String in)
    {
        String newl = "";

        // Split the string on the decimal point.
        String[] parts = in.split("\\.");

        // Reverse the part before the decimal point and add commas every three digits.
        for (int a = parts[0].length() - 1; a >= 0; a--)
        {
            newl = parts[0].charAt(a) + newl;
            if ((parts[0].length() - a) % 3 == 0 && a > 0)
                newl = "," + newl;
        }

        // Ensure the part after the decimal point has exactly two decimal places.
        String afterDecimal = "";
        try
        {
            afterDecimal = parts[1].length() >= 2 ? parts[1].substring(0, 2) : parts[1] + "0";
        }
        catch (Exception e)
        {
            System.out.println(e);
            afterDecimal = "00";
        }

        // Add the part after the decimal point to the result.
        newl = newl + "." + afterDecimal;

        return newl;
    }

}
