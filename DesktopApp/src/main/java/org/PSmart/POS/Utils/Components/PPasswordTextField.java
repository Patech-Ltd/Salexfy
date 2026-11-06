package org.PSmart.POS.Utils.Components;

//import antlr.Utils;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.TransparentBottomBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class PPasswordTextField extends JPanel {

    private Color color;

    private String placeholder;

    private BufferedImage iconImage;

    private JLabel label;

    private JTextField jTextField;

    private JPasswordField jPasswordField;

    private Icon icon;

    private Color txtColor;

    private JLabel iconLabel;

    private JPanel linear;

    private Image openeyeimage;

    private Image closeeyeimage;


    private Boolean showPassword=false;




    public PPasswordTextField(Color color, Color txtColor, String placeholder) {
        this.color = color;
        this.txtColor=txtColor;
        this.placeholder = placeholder;
        draw();
    }

    private void draw() {

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

        jPasswordField=new JPasswordField();

        jPasswordField.setVisible(true);
        linear.add(jPasswordField);
        jPasswordField.setText("");
        jPasswordField.setLayout(null);
        jPasswordField.setFont(new Font("Courier",Font.BOLD,20));
        jPasswordField.setForeground(txtColor);
        jPasswordField.setOpaque(false);
        jPasswordField.setBorder(new TransparentBottomBorder(color));
        jPasswordField.setCaretColor(color);


        try {
            iconImage = IconsUtils.getIcon("open_eye.png");
            openeyeimage = iconImage.getScaledInstance(35,35, Image.SCALE_SMOOTH);

            iconImage = IconsUtils.getIcon("close_eye.png");
            closeeyeimage = iconImage.getScaledInstance(35,35, Image.SCALE_SMOOTH);
            if (iconImage != null) {

                icon = new ImageIcon(openeyeimage);
                if (icon != null) {
                    iconLabel = new JLabel(icon);
                    linear.add(iconLabel);
                }
            }

            if(iconLabel!=null) {

                jTextField.setVisible(false);
                linear.repaint();
                iconLabel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        showPassword=!showPassword;
                        if(showPassword)
                        {
                            jPasswordField.setVisible(false);
                            jTextField.setVisible(true);
                            iconLabel.setIcon(new ImageIcon(closeeyeimage));
                            linear.repaint();
                            jTextField.setText(jPasswordField.getText());
                            jTextField.setCaretPosition(jPasswordField.getCaretPosition());
                            jTextField.grabFocus();
                        }
                        else
                        {
                            jPasswordField.setVisible(true);
                            jTextField.setVisible(false);
                            iconLabel.setIcon(new ImageIcon(openeyeimage));
                            linear.repaint();
                            jPasswordField.setText(jTextField.getText());
                            jPasswordField.setCaretPosition(jTextField.getCaretPosition());
                            jPasswordField.grabFocus();
                        }



                    }
                });
            }

        }catch (Exception es)
        {
            es.printStackTrace();
        }



        add(linear);
    }

    public void setText(String str)
    {
        jTextField.setText(str);
        jPasswordField.setText(str);
    }

    public JTextField getTextField()
    {
        return jTextField;
    }

    public String getText()
    {
        if(showPassword)
            return jTextField.getText();
        else
            return jPasswordField.getText();
    }

    public void setAction(Runnable runnable)
    {
        jTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if(e.getKeyCode()==KeyEvent.VK_ENTER)
                    runnable.run();
            }
        });

        jPasswordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if(e.getKeyCode()==KeyEvent.VK_ENTER)
                    runnable.run();
            }
        });
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, 65);
        label.setSize(width,25);

        linear.setLocation(0,25);
        linear.setSize(width,35);

        int left=0;
        if(iconLabel!=null) {
//            final Image image = iconImage.getScaledInstance(35,35, Image.SCALE_SMOOTH);
//            iconLabel.setIcon(new ImageIcon(openeyeimage));
            iconLabel.setSize(35, 35);
            iconLabel.setLocation(width-35, 0);
            left=40;
        }
        jTextField.setSize(width-left,35);
        jTextField.setLocation(0,0);

        jPasswordField.setSize(width-left,35);
        jPasswordField.setLocation(0,0);






    }


    public void grabFocus()
    {
        if(showPassword)
            jTextField.grabFocus();
        else
            jPasswordField.grabFocus();
    }




}
