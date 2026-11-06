package org.PSmart.POS.Utils.Components;

import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.Utils.TransparentBottomBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;


public class PComboBox extends JPanel {

    private Color color;

    private String placeholder;

    private BufferedImage iconImage;

    private JLabel label;

    private JComboBox<Object> comboBox;

    private Icon icon;

    private Color txtColor;

    private JLabel iconLabel;

    private JPanel linear;


    public PComboBox(Color color, Color txtColor, String placeholder) {
        this.color = color;
        this.txtColor=txtColor;
        this.placeholder = placeholder;
//        draw();
    }



    public PComboBox(Color color, Color txtColor, String placeholder, BufferedImage iconImage) {
        this.color = color;
        this.txtColor=txtColor;
        this.placeholder = placeholder;
        if(iconImage!=null) {
            this.iconImage = iconImage;
            this.icon = new ImageIcon(iconImage);
        }
//        draw();
    }

    protected void draw() {

        setLayout(null);
        label=new JLabel(placeholder);
        label.setFont(new Font("Courier",Font.BOLD,20));
        label.setForeground(color);
        label.setVisible(true);


        add(label);

        linear=new JPanel();
        linear.setLayout(null);
        linear.setOpaque(false);
        setOpaque(false);
        if(icon!=null)
        {
            iconLabel=new JLabel(icon);
            linear.add(iconLabel);
        }

        if(comboBox==null)
            comboBox =new JComboBox<>();

        comboBox.setVisible(true);
        linear.add(comboBox);
        comboBox.setLayout(null);
        comboBox.setFont(new Font("Courier",Font.BOLD,20));
        comboBox.setForeground(txtColor);
//        comboBox.setText("");
        comboBox.setOpaque(false);
        comboBox.setBorder(new TransparentBottomBorder(color));

//        comboBox.setBground(color);

        add(linear);

    }

    public void setComboBox(JComboBox<Object> comboBox)
    {
        this.comboBox=comboBox;
    }

    public JComboBox<Object> getComboBox()
    {
        return comboBox;
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, 65);
        label.setSize(width,25);

        linear.setLocation(0,25);
        linear.setSize(width,35);

        int left=0;
        if(iconLabel!=null) {
            final Image image = iconImage.getScaledInstance(35,35, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(image));
            iconLabel.setSize(35, 35);
            iconLabel.setLocation(width-35, 0);
            left=40;
        }
        comboBox.setSize(width-left,35);
        comboBox.setLocation(0,0);

    }

    public void setAction(Runnable runnable) {
        comboBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    runnable.run();
            }
        });

    }


    public void iconAction(Runnable runnable)
    {
        if(iconLabel!=null)
        {
            iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    runnable.run();
                }
            });
        }
    }



}

