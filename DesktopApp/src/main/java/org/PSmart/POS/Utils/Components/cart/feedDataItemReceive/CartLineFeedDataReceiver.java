package org.PSmart.POS.Utils.Components.cart.feedDataItemReceive;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Entities.StockFeedItem;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.MyUtils;
import org.hibernate.Session;
import org.jdatepicker.JDatePanel;
import org.jdatepicker.JDatePicker;
import org.jdatepicker.UtilDateModel;
import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class CartLineFeedDataReceiver extends JPanel {
    private StockFeedItem stockFeedItem;

    private Session dbsession;

    private User loggedUser;

    private JTextField itemName;

    private JTextField quantity;

    private JTextField buyingPrice;

    private JTextField sellingPrice;


    private JDatePicker expiry;

    public CartLineFeedDataReceiver(StockFeedItem stockFeedItem, Session dbsession, User loggedUser) {
        this.stockFeedItem = stockFeedItem;
        this.dbsession = dbsession;
        this.loggedUser = loggedUser;

        if(stockFeedItem!=null)
        {

            drawData();
            drawSize();

        }

    }

    public void drawSize() {

        setLayout(null);

        int unitSize=getWidth()/8-10;

        itemName.setSize(unitSize*3,getHeight());
        itemName.setLocation(0,10);
        itemName.setForeground(new Color(0,0,128));
        itemName.setFont(MyUtils.courierFont(15));
        add(itemName);

        quantity.setSize(unitSize,getHeight());
        quantity.setLocation(itemName.getWidth()+2,10);
        add(quantity);

        buyingPrice.setSize(quantity.getSize());
        sellingPrice.setSize(quantity.getSize());

        buyingPrice.setLocation(quantity.getLocation().x+quantity.getWidth()+2,10);
        add(buyingPrice);

        sellingPrice.setLocation(buyingPrice.getLocation().x+buyingPrice.getWidth()+2,10);
        add(sellingPrice);

        expiry.setSize(unitSize*2,getHeight());
        expiry.setLocation(getWidth()-expiry.getWidth()-1,getHeight()/2-5);
        add(expiry);

        invalidate();
        revalidate();


    }

    private void drawData() {
        itemName=new JTextField();
        itemName.setEditable(false);

        buyingPrice=new JTextField();
        quantity=new JTextField();
        sellingPrice=new JTextField();

        UtilDateModel model = new UtilDateModel();
        Properties properties = new Properties();
        properties.put("text.today", "Today");
        properties.put("text.month", "Month");
        properties.put("text.year", "Year");

        if(stockFeedItem.getExpiry()==null)
            model.setValue(Calendar.getInstance().getTime());
        else
            model.setValue(new Date(stockFeedItem.getExpiry()));


       expiry = new JDatePicker(model,"E, dd-MMM-YYYY");

        if(stockFeedItem.getItem()!=null) {
            Item ourItem = stockFeedItem.getItem();
            itemName.setText(stockFeedItem.getItem().getName());
            if (stockFeedItem.getSellingPrice() == null)
            {

                buyingPrice.setText(ourItem.getBuyingPrice().toString());
                sellingPrice.setText(ourItem.getSellingPrice().toString());
                quantity.setText("1");
                quantity.grabFocus();
            }
            else{
                buyingPrice.setText(stockFeedItem.getBuyingPrice().toString());
                sellingPrice.setText(stockFeedItem.getSellingPrice().toString());
                quantity.setText(stockFeedItem.getQuantity().toString());

//                (()expiry.getModel()).setValue(stockFeedItem.getExpiry());
            }
        }

    }

    public StockFeedItem getStockFeedItem() {

        stockFeedItem.setQuantity(getDoubles(quantity));
        stockFeedItem.setBuyingPrice(getDoubles(buyingPrice));
        stockFeedItem.setSellingPrice(getDoubles(sellingPrice));

        stockFeedItem.setExpiry(((Date)expiry.getModel().getValue()).getTime());



        return stockFeedItem;
    }

    Double getDoubles(JTextField textField){
        return MyUtils.getDouble(textField.getText().trim());
    }

    public JTextField getQuantity() {
        return quantity;
    }

    public JTextField getBuyingPrice() {
        return buyingPrice;
    }

    public JTextField getSellingPrice() {
        return sellingPrice;
    }

    public JDatePicker getExpiry() {
        return expiry;
    }


}
