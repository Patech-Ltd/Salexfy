package org.PSmart.POS.ui.MainView.ui.stock;

import org.PSmart.POS.ui.MainView.DashBoard;
import org.PSmart.POS.ui.MainView.ui.stock.Categories.CategoriesPanel;

import org.PSmart.POS.ui.MainView.ui.stock.Items.ItemsPanel;

import org.PSmart.POS.ui.MainView.ui.stock.stockfeed.GoodsReceivedPanel;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public class StockPanel extends JPanel {

    private DashBoard dashBoard;

    private JPanel sidebar;

    private Button[] sidebuttons;

    private JPanel playground;
    public StockPanel(DashBoard dashBoard)
    {
        super();
        this.dashBoard=dashBoard;
        draw();
    }

    private void draw() {
        setLocation(0,0);
        setSize(new Dimension(dashBoard.getPlayground().getWidth(),dashBoard.getPlayground().getHeight()));
        setBackground(new Color(191,191,191));
        repaint();
        System.out.println("opened Stock");

        setLayout(null);
        addSidebars();

        playground=new JPanel();
        playground.setLayout(null);
        playground.setBackground(new Color(191, 191, 191, 128));
        playground.setSize(getWidth()-sidebar.getWidth(),getHeight());
        playground.setLocation(sidebar.getWidth(),0);

        add(playground);



    }

    private JButton[] sideMenus;
    private void addSidebars() {

        String[][] sidebars={
                {"Current Stock","cstock.png"},
                {"All Items","items.png"},
                {"Items Categories","categories.png"},
                {"Purchases/<br>Stock Feed","purchase.png"}
        };
        sideMenus=new JButton[sidebars.length];
        Color sidebarColor=new Color(0,0,128);


        int sidebar_height=70;
        int sidebar_width=getWidth()/10;
        sidebar=new JPanel();
        sidebar.setBackground(new Color(39,144,255));

//        sidebar.setLocation(2,30);
        sidebar.setSize(sidebar_width,sidebar_height*sidebars.length);
        sidebar.setLocation(0,(getHeight()-sidebar.getHeight())/2);
        sidebar.setLayout(new GridLayout(sidebars.length,1));
        add(sidebar);

        for(int i=0;i<sidebars.length;i++)
        {
            sideMenus[i]=new JButton("<html><center>"+sidebars[i][0]+"</html>");


            sideMenus[i].setBackground(sidebarColor);
            sideMenus[i].setForeground(Color.WHITE);
            sideMenus[i].setFont(MyUtils.arialFont(18));
            sidebar.add(sideMenus[i]);
            int finalI = i;
            sideMenus[i].addActionListener((e)->{
                setActiveTab(finalI);
                getRunnables()[finalI].run();
            });

            try {
                int height=20;
                BufferedImage img= IconsUtils.getIcon(sidebars[i][1]);

                Image image = null;
                if(img!=null)
                    image = img.getScaledInstance(img.getWidth()*height/img.getHeight(),height, Image.SCALE_SMOOTH);

                if(image!=null)
                    sideMenus[i].setIcon(new ImageIcon(image));

            }catch (Exception es)
            {
                es.printStackTrace();
            }

        }

        repaint();


    }

    public void setActiveTab(int tab)
    {
        for(int i=0;i<sideMenus.length;i++)
        {
            if(i==tab)
                sideMenus[i].setBackground(new Color(39, 144, 255));
            else
                sideMenus[i].setBackground(new Color(0,0,128));

        }

    }


    private HashMap<String,JPanel> fragments=new HashMap<>();

    public Runnable[] getRunnables()
    {
        return new Runnable[]{
                (()->{

                }),
                (() -> {
                    String key="items";
                    playground.removeAll();
                    if(!fragments.containsKey(key))
                        fragments.put(key,new ItemsPanel(this));
                    playground.add(fragments.get(key));
                    playground.repaint();

                }),
                (() -> {

                    String key="categories";
                    playground.removeAll();
                    if(!fragments.containsKey(key))
                        fragments.put(key,new CategoriesPanel(this));
                    playground.add(fragments.get(key));
                    playground.repaint();

                }),
                (() -> {
                    String key="store_feed";
                    playground.removeAll();
                    if(!fragments.containsKey(key))
                        fragments.put(key,new GoodsReceivedPanel(this));
                    playground.add(fragments.get(key));
                    playground.repaint();

                }),
                (() -> {

                }),
                (() -> {

                }),
                (() -> {

                }),
                (() -> {

                }),
                (() -> {

                })


        };

    }

    public DashBoard getDashBoard() {
        return dashBoard;
    }

    public JPanel getPlayground() {
        return playground;
    }
}
