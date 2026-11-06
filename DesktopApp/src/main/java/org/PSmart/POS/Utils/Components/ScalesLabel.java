package org.PSmart.POS.Utils.Components;


import org.PSmart.POS.Utils.DecimalDocumentFilter;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

public class ScalesLabel extends JPanel {

    private JLabel itemsField;

    private JLabel nameField;

    private JLabel priceField;

    public ScalesLabel(String first,String second,String third)
    {

        super();
        itemsField=new JLabel(first,JLabel.CENTER);
        nameField=new JLabel(second,JLabel.CENTER);
        priceField=new JLabel(third,JLabel.CENTER);

        setLayout(null);
//        itemsField.setFont(MyUtils.courierFont());
        setFonts(itemsField,priceField);

        addAllComponents(itemsField,nameField,priceField);
    }

    private void addAllComponents(JLabel ... fields) {
        for(JLabel i:fields)
        {

            add(i);
        }
    }

    private void setFonts(JLabel ... fields) {
        for(JLabel i:fields)
        {
            i.setFont(MyUtils.courierFont(12));

        }
    }

    @Override
    public void setSize(Dimension d)
    {
        setSize(d.width,d.height);
    }




    @Override
    public void setSize(int width,int height)
    {
        super.setSize(width,height);
        itemsField.setSize(width/6,height);
        itemsField.setLocation(0,0);

        nameField.setSize(width*2/3-6,height);
        nameField.setLocation(itemsField.getWidth()+3,0);

        priceField.setSize(width/6,height);
        priceField.setLocation(width-priceField.getWidth(),0);


    }
}

