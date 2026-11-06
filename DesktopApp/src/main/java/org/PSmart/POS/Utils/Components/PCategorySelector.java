package org.PSmart.POS.Utils.Components;



import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.Repositories.ItemCategoryRepository;
import org.PSmart.POS.Utils.TransparentBottomBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;


public class PCategorySelector extends PComboBox {

    private Color color;

    private String placeholder;

    private BufferedImage iconImage;

    private JLabel label;

    private JComboBox<ItemCategory> comboBox;

    private Icon icon;

    private Color txtColor;

    private JLabel iconLabel;

    private JPanel linear;




    public PCategorySelector(Color color, Color txtColor, String placeholder) {
        super(color, txtColor, placeholder);
//        setModel();
        this.draw();
    }

    public PCategorySelector(Color color, Color txtColor, String placeholder, BufferedImage iconImage) {
        super(color, txtColor, placeholder, iconImage);
//        setModel();
        this.draw();
    }




    @Override
    protected void draw() {



        System.out.println("worked");
        categorylist= ItemCategoryRepository.findAll();
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


        comboBox = new JComboBox<ItemCategory>();
        comboBox.addItem(ItemCategory.builder().name("Default").build());
        for(ItemCategory i:categorylist)
            comboBox.addItem(i);

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
        super.draw();

    }

    private void setModel() {




    }


    private List<ItemCategory> categorylist;

    public JComboBox<ItemCategory> getICComboBox()
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








}

