package org.PSmart.POS.ui.MainView.ui.stock.Items;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.ui.MainView.ui.stock.Items.popups.EditItem;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.ItemComponent.ItemJPanel;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.Components.PTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class ItemsPanel extends JPanel {

    private StockPanel stockPanel;

    private JButton addNew;


    private PTextField search;

    private JPanel playground;

    private java.util.List<Item> items;//= ItemRepository.findAll();

    public static EditItem itemeditor;


    public ItemsPanel(StockPanel stockPanel) {
        this.stockPanel =stockPanel;

        draw();
    }

    private void draw() {
        setLocation(3,0);
        setSize(stockPanel.getPlayground().getSize());
        setLayout(null);
        setBackground(new Color(191,191,191));
        addNew=new JButton("+   Add New Item");
        addNew.setBackground(new Color(52,58,64));
        addNew.setFont(MyUtils.courierFont(20));
        addNew.setForeground(Color.WHITE);

        addNew.setLocation(getWidth()/4,10);
        addNew.setSize(getWidth()/2,35);

        addNew.addActionListener(e -> {
            if(itemeditor!=null)
                itemeditor.dispose();
            itemeditor=new EditItem(null,stockPanel.getDashBoard().getDbsession(),stockPanel.getDashBoard().getLoggedUser());
            itemeditor.setSaveAction(()->{
                drawItems();
            });

        });

        add(addNew);

        search=new PTextField(new Color(52,58,64),new Color(0,0,255),"Search Item", IconsUtils.getIcon("search.png"));

        search.setLocation(getWidth()/6,50);
        search.setSize(getWidth()-getWidth()/3,60);
//        search.setBackground(Color.WHITE);
//        search.setOpaque(true);
        add(search);


        drawItems();

        search.getField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                new Thread(()->{
                    filterItems(search.getText());
                }).start();

            }
        });


        repaint();


    }

    private JScrollPane scrollPane;

    public void drawItems() {

        try {
            if (scrollPane != null)
                remove(scrollPane);
        }catch (Exception es)
        {

        }


        items=ItemRepository.findAll();
        if(search.getText().trim().length()>0)
        {
            filterItems(search.getText().trim());
            return;
        }




        GridLayout layout=new GridLayout(items.size(),1);



        int itempanelHeight=50;
        int vgap=5;

        layout.setVgap(vgap);

        playground=new JPanel(layout);

        scrollPane=new JScrollPane(playground);

        scrollPane.setLocation(0,140);
        scrollPane.setSize(getWidth(),getHeight()-150);

        add(scrollPane);

        playground.setSize(getWidth(),(itempanelHeight+vgap)*items.size());
        playground.setPreferredSize(new Dimension(getWidth()-40,(itempanelHeight+vgap)*items.size()));

        if(getHeight()-130>(itempanelHeight+vgap)*items.size()) {
            scrollPane.setPreferredSize(new Dimension(getWidth()-40, (itempanelHeight + vgap) * items.size()));
            scrollPane.setSize(new Dimension(getWidth()-40, (itempanelHeight + vgap) * items.size()));
        }

        for(Item i:items) {

            ItemJPanel itemsPanel=new ItemJPanel(i, stockPanel.getDashBoard().getDbsession(), stockPanel.getDashBoard().getLoggedUser());
            itemsPanel.itemsPanel=this;
            itemsPanel.setSize(getWidth(),itempanelHeight);
            itemsPanel.setPreferredSize(new Dimension(getWidth()-40,itempanelHeight));
            playground.add(itemsPanel);

            itemsPanel.clickRunnable=(()->{
                if(itemeditor!=null)
                    itemeditor.dispose();

                itemeditor=new EditItem(i,stockPanel.getDashBoard().getDbsession(),stockPanel.getDashBoard().getLoggedUser());
                itemeditor.setSaveAction(()->{
                    itemsPanel.setItem(ItemRepository.findById(i.getUid()));
                    itemsPanel.feedData();
                });

                itemeditor.setDeleteAction(()->{
                    itemeditor.dispose();
                    drawItems();
                });
                itemeditor.setVisible(true);
                itemeditor.repaint();

            });

        }

        playground.setBackground(new Color(191,191,191));
        scrollPane.setBackground(new Color(191,191,191));

        repaint();
    }

    public void filterItems(String str) {

        try {
            if (scrollPane != null)
                remove(scrollPane);
        }catch (Exception es)
        {

        }

        java.util.List<Item> filteredItems=new ArrayList<>();
        for(Item i:items)
            if(i.getSearchString().contains(str.toLowerCase()))
                filteredItems.add(i);

        GridLayout layout=new GridLayout(filteredItems.size(),1);



        int itempanelHeight=50;
        int vgap=5;

        layout.setVgap(vgap);

        playground=new JPanel(layout);

        scrollPane=new JScrollPane(playground);

        scrollPane.setLocation(0,140);
        scrollPane.setSize(getWidth(),getHeight()-150);

        add(scrollPane);

        playground.setSize(getWidth(),(itempanelHeight+vgap)*filteredItems.size());
        playground.setPreferredSize(new Dimension(getWidth()-40,(itempanelHeight+vgap)*filteredItems.size()));

        if(getHeight()-130>(itempanelHeight+vgap)*filteredItems.size()) {
            scrollPane.setPreferredSize(new Dimension(getWidth()-40, (itempanelHeight + vgap+10) * filteredItems.size()));
            scrollPane.setSize(new Dimension(getWidth()-40, (itempanelHeight + vgap+10) * filteredItems.size()));
        }

        for(Item i:filteredItems) {

            final ItemJPanel filteredItemsPanel=new ItemJPanel(i, stockPanel.getDashBoard().getDbsession(), stockPanel.getDashBoard().getLoggedUser());
            filteredItemsPanel.itemsPanel=this;
            filteredItemsPanel.setSize(getWidth(),itempanelHeight);
            filteredItemsPanel.setPreferredSize(new Dimension(getWidth()-40,itempanelHeight));
            playground.add(filteredItemsPanel);
            filteredItemsPanel.clickRunnable=(()->{
                if(itemeditor!=null)
                    itemeditor.dispose();

                itemeditor=new EditItem(i,stockPanel.getDashBoard().getDbsession(),stockPanel.getDashBoard().getLoggedUser());
                itemeditor.setSaveAction(()->{
                    filteredItemsPanel.setItem(ItemRepository.findById(i.getUid()));
                    filteredItemsPanel.feedData();
                });

                itemeditor.setDeleteAction(()->{
                    itemeditor.dispose();
                    drawItems();
                });
                itemeditor.setVisible(true);
                itemeditor.repaint();

            });

        }

        playground.setBackground(new Color(191,191,191));
        scrollPane.setBackground(new Color(191,191,191));

        repaint();
    }
}
