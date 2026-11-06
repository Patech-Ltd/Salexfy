package org.PSmart.POS.Utils.Components.ItemComponent;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.ui.MainView.ui.stock.Items.ItemsPanel;
import org.PSmart.POS.Utils.Components.ImageLabel;
import org.PSmart.POS.Utils.ImageResultRunnable;
import org.PSmart.POS.Utils.MyUtils;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ItemJPanel extends JPanel {

    public ItemsPanel itemsPanel;
    private Session dbsession;
    private Item item;

    private ImageLabel imageLabel;

    private JLabel productname;

    private JLabel price;

    private JLabel categoryname;

    private JLabel description;

    private User loggedUser;

    public ItemJPanel(Item item, Session dbsession, User loggedUser) {
        super();

        this.item = item;
        this.dbsession=dbsession;
        this.loggedUser=loggedUser;

        feedData();
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
        productname=new JLabel(code+item.getName());
        description=new JLabel(item.getDescription());
        price=new JLabel(MyUtils.formatCurrency(item.getSellingPrice().toString()));
        categoryname=new JLabel(item.getCategory()==null?"Default":item.getCategory().getName());

        imageLabel=new ImageLabel(null,60,60);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if(clickRunnable!=null)
                    clickRunnable.run();

                System.out.println("item pressed");

            }
        });
        setBackground(new Color(255,255,255));
        draw();

        item.getImage(new ImageResultRunnable() {
            @Override
            public void run() {
                imageLabel.setImage(getBufferedImage());
            }
        });



    }
    private JPanel playground;


    public Runnable clickRunnable;

    private void draw() {

        setLayout(new BorderLayout());
        add(imageLabel,BorderLayout.WEST);

        playground=new JPanel(new GridLayout(2,2));
//        playground.setLayout(new GridLayout(2,2));

        add(playground,BorderLayout.CENTER);

        playground.add(productname);
        productname.setFont(MyUtils.courierFont(15));
        playground.add(description);

        playground.add(price);
        price.setFont(MyUtils.courierFont(18));
        price.setForeground(Color.BLUE);
        playground.add(categoryname);
        categoryname.setForeground(Color.BLUE);

    }


    public void setItem(Item byId) {
        this.item=byId;
    }
}
