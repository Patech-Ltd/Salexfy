package org.PSmart.POS.Utils.Components.GridSearch.ItemComponents;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.ui.MainView.ui.stock.Items.ItemsPanel;
import org.PSmart.POS.Utils.Components.ImageLabel;
import org.PSmart.POS.Utils.DecimalDocumentFilter;
import org.PSmart.POS.Utils.ImageResultRunnable;
import org.PSmart.POS.Utils.MyUtils;
import org.hibernate.Session;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;

public class GridItemJpanel extends JPanel {

    public ItemsPanel itemsPanel;
    private Session dbsession;
    private Item item;

    private ImageLabel imageLabel;

    private JLabel productname;

    private JLabel price;

    private JLabel categoryname;

    private JLabel description;

    private User loggedUser;

    public GridItemJpanel(Item item, Session dbsession, User loggedUser) {
        super();

        this.item = item;
        this.dbsession=dbsession;
        this.loggedUser=loggedUser;

//        setSize(width,height);
//        setPreferredSize(new Dimension(width,height));
        feedData();

//        setBackground(Color.WHITE);



    }

    private int width=160;

    private int height=100;



    private JPanel playground;


    public Runnable clickRunnable;

    public Runnable actionRunnable;

    private void draw() {

        setLayout(null);
//        add(imageLabel,BorderLayout.CENTER);

        playground=new JPanel(new BorderLayout());
//        playground.setLayout(new GridLayout(2,2));
        playground.setSize(width,height);
        playground.setPreferredSize(new Dimension(width,height));
        playground.setLocation(0,0);

        add(playground);

        playground.add(productname,BorderLayout.SOUTH);
        productname.setFont(MyUtils.courierFont(14));
//        playground.add(description);

//        playground.add(price);
        price.setFont(MyUtils.courierFont(14));
        price.setForeground(Color.BLUE);
//        playground.add(categoryname);
        categoryname.setForeground(Color.BLUE);


        raiter=new JPanel(new GridLayout(4,1));
        raiter.setSize(width/2,height);
        playground.add(raiter,BorderLayout.EAST);

        plus=new JButton(" + ");
        raiter.add(plus);

        quantity=new JTextField(5);
        ((AbstractDocument) quantity.getDocument()).setDocumentFilter(new DecimalDocumentFilter());
        raiter.add(quantity);

        minus=new JButton(" - ");
        raiter.add(minus);

        raiter.add(price);

//        int height=getHeight();
//        int width=height+10;
//        setSize(width,height);

//        setPreferredSize(new Dimension(width,height));

        playground.add(imageLabel,BorderLayout.CENTER);

        plus.addActionListener((e -> {
            plus();
        }));

        minus.addActionListener(e -> {
            minus();
        });

    }



    private JPanel raiter;

    private JButton plus;

    private JButton minus;

    private JTextField quantity;




    public void setItem(Item byId) {
        this.item=byId;
    }

    public void plus()
    {
        Double curr=MyUtils.getDouble(quantity.getText());
        curr+=1.0;

//        try {
//            ((AbstractDocument)quantity.getDocument()).replace(0, curr.toString().length(), curr.toString(), null);
//        } catch (BadLocationException e) {
////            throw new RuntimeException(e);
//            e.printStackTrace();
//        }

        try {
            ((AbstractDocument)quantity.getDocument()).remove(0,quantity.getText().length());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        quantity.setText(curr.toString());
        if(actionRunnable!=null)
            actionRunnable.run();
    }

    public void minus()
    {
        Double curr=MyUtils.getDouble(quantity.getText());
        curr-=1.0;

//        try {
//            ((AbstractDocument)quantity.getDocument()).replace(0, curr.toString().length(), curr.toString(), null);
//        } catch (BadLocationException e) {
////            throw new RuntimeException(e);
//            e.printStackTrace();
//        }

        try {
            ((AbstractDocument)quantity.getDocument()).remove(0,quantity.getText().length());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        quantity.setText(curr.toString());
        if(actionRunnable!=null)
            actionRunnable.run();
    }

    public JTextField getQuantity() {
        return quantity;
    }

    public void feedData()
    {
        try{

            removeAll();
            repaint();
            for(MouseListener i:getMouseListeners())
                removeMouseListener(i);

        }catch (Exception ed)
        {

        }

        if(item==null)
        {
            setSize(getWidth(),0);
            setPreferredSize(new Dimension(getWidth(),0));

            return;
        }

        String code="";
        if(item.getBarcode()!=null&&item.getBarcode().trim().length()>0)
            code=item.getBarcode()+" - ";
        productname=new JLabel(code+item.getName(),JLabel.CENTER);
        description=new JLabel(item.getDescription());
        price=new JLabel(MyUtils.formatCurrency(item.getSellingPrice().toString()));
        categoryname=new JLabel(item.getCategory()==null?"Default":item.getCategory().getName());

        imageLabel=new ImageLabel(null,width/2,height-20);
//        imageLabel.setSize(width/20,height-20);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if(clickRunnable!=null)
                    clickRunnable.run();

                plus();

            }
        });
//        setBackground(new Color(255,255,255));
        draw();

        item.getImage(new ImageResultRunnable() {
            @Override
            public void run() {
//                imageLabel.setSize(width/2,height-20);
                imageLabel.setImage(getBufferedImage());
            }
        });

        quantity.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if(actionRunnable!=null)
                    actionRunnable.run();
            }
        });
    }

    public Item getItem() {
        return item;
    }
}
