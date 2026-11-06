package org.PSmart.POS.Utils.Components;

import org.PSmart.POS.Utils.DecimalDocumentFilter;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;

public class ScalesPanel extends JPanel {

    private JTextField itemsField;

    private JTextField nameField;

    private JTextField priceField;

    public ScalesPanel()
    {
        super();
        itemsField=new JTextField();
        nameField=new JTextField();
        priceField=new JTextField();
        setLayout(null);

//        itemsField.setFont(MyUtils.courierFont());
        setFonts(itemsField,priceField);

        addAllComponents(itemsField,nameField,priceField);
    }

    public ScalesPanel(String first,String second,String third)
    {
        super();
        itemsField=new JTextField(first);
        nameField=new JTextField(second);
        priceField=new JTextField(third);

//        itemsField.setFont(MyUtils.courierFont());
        setFonts(itemsField,priceField);

        addAllComponents(itemsField,nameField,priceField);
    }

    private void addAllComponents(JTextField ... fields) {
        for(JTextField i:fields)
        {

            add(i);
        }
    }

    private void setFonts(JTextField ... fields) {
        for(JTextField i:fields)
        {
            i.setFont(MyUtils.courierFont(15));
            AbstractDocument abstractDocument= (AbstractDocument) i.getDocument();
            abstractDocument.setDocumentFilter(new DecimalDocumentFilter());

        }
    }

    public JTextField getItemsField() {
        return itemsField;
    }

    public void setItemsField(JTextField itemsField) {
        this.itemsField = itemsField;
    }

    public JTextField getNameField() {
        return nameField;
    }

    public void setNameField(JTextField nameField) {
        this.nameField = nameField;
    }

    public JTextField getPriceField() {
        return priceField;
    }

    public void setPriceField(JTextField priceField) {
        this.priceField = priceField;
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

    @Override
    public void setSize(Dimension d)
    {
        setSize(d.width,d.height);
    }


}
