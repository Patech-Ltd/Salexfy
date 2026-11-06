package org.PSmart.POS.Utils.Components;

import org.PSmart.POS.Utils.TransparentBottomBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class PTextField extends JPanel {

    private Color color;

    private String placeholder;

    private BufferedImage iconImage;

    private JLabel label;

    private JTextField jTextField;

    private Icon icon;

    private Color txtColor;

    private JLabel iconLabel;

    private JPanel linear;


    public PTextField(Color color,Color txtColor, String placeholder) {
        this.color = color;
        this.txtColor=txtColor;
        this.placeholder = placeholder;
        draw();
    }



    public PTextField(Color color,Color txtColor, String placeholder, BufferedImage iconImage) {
        this.color = color;
        this.txtColor=txtColor;
        this.placeholder = placeholder;
        if(iconImage!=null) {
            this.iconImage = iconImage;
            this.icon = new ImageIcon(iconImage);
        }
        draw();
    }

    private void draw() {

        setLayout(null);
        label=new JLabel(placeholder);
        label.setFont(new Font("Courier",Font.BOLD,15));
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

        jTextField=new JTextField();
        jTextField.setVisible(true);
        linear.add(jTextField);
        jTextField.setLayout(null);
        jTextField.setFont(new Font("Courier",Font.BOLD,20));
        jTextField.setForeground(txtColor);
        jTextField.setText("");
        jTextField.setOpaque(false);
        jTextField.setBorder(new TransparentBottomBorder(color));

        jTextField.setCaretColor(color);

        add(linear);

    }

    public void setText(String str)
    {
        jTextField.setText(str);
    }

    public JTextField getField()
    {
        return jTextField;
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
        jTextField.setSize(width-left,35);
        jTextField.setLocation(0,0);

    }

    public void setAction(Runnable runnable) {
        jTextField.addKeyListener(new KeyAdapter() {
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

    public String getText() {
        return jTextField.getText();
    }

    @Override
    public void grabFocus() {
        super.grabFocus();
        jTextField.grabFocus();
    }
}
