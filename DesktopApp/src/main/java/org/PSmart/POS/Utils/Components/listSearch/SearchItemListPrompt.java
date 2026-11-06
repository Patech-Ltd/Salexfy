package org.PSmart.POS.Utils.Components.listSearch;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Main;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.ItemComponent.ItemJPanel;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.Runnables.ItemSelectedRunnable;
import org.PSmart.POS.Utils.IconsUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;

public class SearchItemListPrompt extends JFrame {

    private PTextField search;

    private JPanel playground;

    private java.util.List<Item> items;

    public SearchItemListPrompt()
    {
        super();
        items= ItemRepository.findAll();
        draw();
    }
    public static Dimension len=Toolkit.getDefaultToolkit().getScreenSize();


    public ItemSelectedRunnable itemSelectedRunnable;

    public Boolean doInverse=false;

    private void draw() {
        setLayout(new BorderLayout());
        Point p=MouseInfo.getPointerInfo().getLocation();

        if(p.y>len.height/2+50) {
            doInverse = true;
            System.out.println("past half");
        }





        setLocation(p.x,p.y);

        int width= len.width/2;
        int height=len.height-p.y;

        if(doInverse)
        {

            height=p.y;
            setLocation(p.x,0);
        }

        setSize(width,height);

        setVisible(true);

        setTitle("Search Item");

        search=new PTextField(new Color(52,58,64),new Color(0,0,255),"Search Item",
                IconsUtils.getIcon("search.png"));



        search.setLocation(getWidth()/6,5);
        search.setSize(getWidth()-getWidth()/3,60);
        search.setPreferredSize(new Dimension(getWidth()-getWidth()/3,60));
//        search.setBackground(Color.WHITE);
//        search.setOpaque(true);

        JPanel searchPanel=new JPanel();
        searchPanel.setPreferredSize(new Dimension(getWidth()-getWidth()/3,80));
        searchPanel.setSize(getWidth()-getWidth()/3,80);
        searchPanel.setLayout(null);

        searchPanel.add(search);

//        add(searchPanel,BorderLayout.NORTH);
        if(doInverse)
            add(searchPanel,BorderLayout.SOUTH);
        else
            add(searchPanel,BorderLayout.NORTH);

        search.grabFocus();

        filterItems("");

        search.getField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
//                new Thread(()->{
//
//                }).start();
                filterItems(search.getText());

            }
        });


        repaint();

    }



    private HashMap<Long,ItemJPanel> itemsCache= new HashMap<>();
    private JScrollPane scrollPane;
    public void filterItems(String str) {

        int prevheight=0;
        int prevLoc=0;
        try {
            if (scrollPane != null) {
                prevheight=scrollPane.getHeight();
                prevLoc=scrollPane.getLocation().y;
                remove(scrollPane);
            }
        }catch (Exception es)
        {
            es.printStackTrace();

        }


        java.util.List<Item> filteredItems=new ArrayList<>();
        if(str.isEmpty()) {
            filteredItems.clear();
            filteredItems.addAll(items);

        }else {
            filteredItems.clear();
            for (Item i : items)
                if (i.getSearchString().contains(str.toLowerCase()))
                    filteredItems.add(i);
        }
        System.out.println("drawing dem");

        GridLayout layout=new GridLayout(items.size(),1);



        int itempanelHeight=50;
        int vgap=5;

        layout.setVgap(vgap);

        if(playground!=null)
            playground.removeAll();

        playground=new JPanel(layout);

        scrollPane=new JScrollPane(playground);

        if(prevheight>0) {
            scrollPane.setLocation(0, prevLoc);
            scrollPane.setSize(getWidth(), prevheight);
        }


//
//        playground.setSize(getWidth(),(itempanelHeight+vgap)*filteredItems.size());
////        playground.setPreferredSize(new Dimension(getWidth()-40,(itempanelHeight+vgap)*filteredItems.size()));
//
//
//        if(getHeight()-130>(itempanelHeight+vgap)*filteredItems.size()) {
//            scrollPane.setPreferredSize(new Dimension(getWidth()-40, (itempanelHeight + vgap+10) * filteredItems.size()));
//            scrollPane.setSize(new Dimension(getWidth()-40, (itempanelHeight + vgap+10) * filteredItems.size()));
//        }



        for(Item i:filteredItems) {

            final ItemJPanel filteredItemsPanel;

            if(itemsCache.containsKey(i.getUid()))
                filteredItemsPanel=itemsCache.get(i.getUid());
            else {

                filteredItemsPanel = new ItemJPanel(i, null, null);
//            filteredItemsPanel.itemsPanel=this;
                filteredItemsPanel.setSize(getWidth(), itempanelHeight);
                filteredItemsPanel.setPreferredSize(new Dimension(getWidth() - 40, itempanelHeight));

                filteredItemsPanel.clickRunnable = (() -> {

                    if (itemSelectedRunnable != null) {
                        itemSelectedRunnable.setClickedItem(i);
                        itemSelectedRunnable.run();
                    }
                });

//                itemsCache.put(i.getUid(),filteredItemsPanel);
            }

            playground.add(filteredItemsPanel);

        }

        playground.setBackground(new Color(191,191,191));
        scrollPane.setBackground(new Color(191,191,191));

        repaint();
        scrollPane.repaint();

        playground.invalidate();
        playground.revalidate();
        playground.repaint();

        add(scrollPane,BorderLayout.CENTER);
    }


}
