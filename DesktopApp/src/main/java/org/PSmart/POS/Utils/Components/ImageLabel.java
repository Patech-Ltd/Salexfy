package org.PSmart.POS.Utils.Components;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ImageLabel extends JLabel {

    private int cornerRadius;

    public ImageLabel() {
        this(new ImageIcon(), 40, 40);
    }

    public ImageLabel(ImageIcon imageIcon, int width, int height) {
        super(imageIcon);
        this.cornerRadius = 10; // Default corner radius
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
        if(imageIcon==null)
            setIcon(createBlankImage(width,height));
        else
            setIcon(imageIcon); // Initialize with the empty image
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Create a rounded rectangle for the border
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                1, 1, getWidth() - 3, getHeight() - 3, cornerRadius, cornerRadius);

        // Set the foreground color for the border
        g2d.setColor(getForeground());

        // Draw the rounded border
        g2d.draw(roundedRectangle);

        // Clip the graphics to the rounded rectangle
        g2d.clip(roundedRectangle);

        // Call the parent's paintComponent to paint the image
        super.paintComponent(g2d);

        g2d.dispose();
    }

    public void setIcon(ImageIcon icon) {
        super.setIcon(icon);
        repaint(); // Repaint to reflect the changes
    }

    public void setImage(File file) {
        try {
            // Read the image from the file
            BufferedImage bufferedImage = ImageIO.read(file);

            // Calculate the aspect ratio to maintain proportions
            double aspectRatio = (double) bufferedImage.getWidth() / bufferedImage.getHeight();


            // Scale the image to fit the label while maintaining aspect ratio
            int scaledWidth = getWidth();

            if(scaledWidth==0) {
                scaledWidth = 40;
                setSize(40,40);
            }

            int scaledHeight = (int) (scaledWidth / aspectRatio);

            Image scaledImage = bufferedImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            setVerticalAlignment(JLabel.TOP);
            setIcon(new ImageIcon(scaledImage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImage(BufferedImage bufferedImage)
    {
        try {
            // Calculate the aspect ratio to maintain proportions
            double aspectRatio = (double) bufferedImage.getWidth() / bufferedImage.getHeight();

            if(getWidth()==0)
                Thread.sleep(800);

            // Scale the image to fit the label while maintaining aspect ratio
            int scaledWidth = getWidth();

            if(scaledWidth==0) {
                scaledWidth = 40;
                setSize(40,40);
            }

            int scaledHeight = (int) (scaledWidth / aspectRatio);

            Image scaledImage = bufferedImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            setVerticalAlignment(JLabel.TOP);
            setIcon(new ImageIcon(scaledImage));
        }catch (Exception es)
        {
            es.printStackTrace();
        }
    }




    public static ImageIcon createBlankImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Fill the image with a blue color (you can customize the color)
        g2d.setColor(new Color(191,191,191));
        g2d.fillRect(0, 0, width, height);

        g2d.dispose();

        return new ImageIcon(image);
    }
}
