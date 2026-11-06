package org.PSmart.POS.Utils.Components.GridSearch;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.Main;
import org.PSmart.POS.Repositories.ItemCategoryRepository;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.ItemComponent.ItemJPanel;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.Runnables.ItemSelectedRunnable;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.ImageResultRunnable;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CategoricalItemsSearch extends JFrame {

        private PTextField search;

        private JPanel playground;

        private java.util.List<Item> items;

        private java.util.List<ItemCategory> categories;

        private HashMap<String, List<Item>> categoriesItems=new HashMap<>();

        private static Dimension len=Toolkit.getDefaultToolkit().getScreenSize();

        public CategoricalItemsSearch()
        {
            super();
            items= ItemRepository.findAllAsc();
            categories = ItemCategoryRepository.findAllAsc();
            categoriesMenu=new JButton[categories.size()+1];
            arrangeInCategories();
            draw();
        }

    private void arrangeInCategories() {
            categoriesItems.put("All",items);
            for(Item i:items)
            {
                String nm=i.getCategory().getName();
                List<Item> found=categoriesItems.get(nm);
                if(found==null) {
                    found = new ArrayList<Item>();
                    categoriesItems.put(nm,found);
                }
                found.add(i);
            }
    }

//    public static Dimension len=Toolkit.getDefaultToolkit().getScreenSize();


        public ItemSelectedRunnable itemSelectedRunnable;

        private Boolean doInverse=false;

        private void draw() {
            setLayout(new BorderLayout());
            Point p=MouseInfo.getPointerInfo().getLocation();

            if(p.y>len.height/2+100) {
                doInverse = true;
                System.out.println("past half");
            }

            int width= len.width/2;
            int height=len.height-p.y;
            setLocation(p.x,p.y);
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



            search.setLocation(getWidth()/6,50);
            search.setSize(getWidth()-getWidth()/3,60);
//        search.setBackground(Color.WHITE);
//        search.setOpaque(true);
            if(doInverse)
                add(search,BorderLayout.SOUTH);
            else
                add(search,BorderLayout.NORTH);


            filterItems("");

            search.getField().addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    super.keyReleased(e);
                    new Thread(()->{
                        filterItems(search.getText());
                    }).start();

                }
            });


            addCategoriesPannel();

            repaint();


        }

        private JPanel categoriesPanel;

        private JScrollPane categoriesScrollPane;

    private void addCategoriesPannel() {

        categoriesPanel=new JPanel();
        categoriesScrollPane=new JScrollPane(categoriesPanel);

        add(categoriesPanel,BorderLayout.WEST);

        int width=150;
        int height=80;


        categoriesMenu[0]= MyUtils.createButton("All",new Color(0,0,139));
        categoriesMenu[0].setSize(width,80);
        categoriesMenu[0].setForeground(Color.WHITE);
        categoriesMenu[0].setFont(MyUtils.courierFont(20));

        categoriesPanel.add(categoriesMenu[0]);

        categoriesMenu[0].addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setActiveTab(0);
                selectedCategory="All";
                filterItems(search.getText());
            }
        });

        for(int i=1;i<=categories.size();i++)
        {
            ItemCategory cc=categories.get(i);
            categoriesMenu[i]= MyUtils.createButton(cc.getName(),new Color(0,0,139));
            categoriesMenu[i].setSize(width,80);
            categoriesMenu[i].setForeground(Color.WHITE);
            categoriesMenu[i].setFont(MyUtils.courierFont(20));

            categoriesPanel.add(categoriesMenu[i]);

            int finalI = i;
            categoriesMenu[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setActiveTab(finalI);
                    selectedCategory=cc.getName();
                    filterItems(search.getText());
                }
            });

            cc.getImage(new ImageResultRunnable() {
                @Override
                public void run() {

                    final Image image = getBufferedImage().getScaledInstance(getBufferedImage().getWidth()*60/getBufferedImage().getHeight(),60, Image.SCALE_SMOOTH);
                    if(image!=null)
                        categoriesMenu[finalI].setIcon(new ImageIcon(image));

                    categoriesMenu[finalI].setFont(MyUtils.courierFont(15));
                    categoriesMenu[finalI].setHorizontalTextPosition(SwingConstants.CENTER);
                    categoriesMenu[finalI].setVerticalTextPosition(SwingConstants.BOTTOM);

                }
            });

        }








    }


    public void setActiveTab(int tab)
    {
        for(int i=0;i<categoriesMenu.length;i++)
        {
            if(i!=tab)
                categoriesMenu[i].setBackground(new Color(25, 118, 211));
            else
                categoriesMenu[i].setBackground(new Color(0,0,139));

        }

    }




    private JButton[] categoriesMenu;

    private String selectedCategory="All";


    private JScrollPane scrollPane;












        public void filterItems(String str) {

            try {
                if (scrollPane != null)
                    remove(scrollPane);
            }catch (Exception es)
            {

            }
            if(selectedCategory==null)
                selectedCategory="All";

            List<Item> currentList=categoriesItems.get(selectedCategory);

            if(currentList==null)
                currentList=items;

            java.util.List<Item> filteredItems=new ArrayList<>();
            if(str.isEmpty())
                filteredItems.addAll(currentList);
            else {
                for (Item i : currentList)
                    if (i.getSearchString().contains(str.toLowerCase()))
                        filteredItems.add(i);
            }

            GridLayout layout=new GridLayout(filteredItems.size(),1);



            int itempanelHeight=50;
            int vgap=5;

            layout.setVgap(vgap);

            playground=new JPanel(layout);

            scrollPane=new JScrollPane(playground);

            scrollPane.setLocation(0,140);
            scrollPane.setSize(getWidth(),getHeight()-150);

            add(scrollPane,BorderLayout.CENTER);

            playground.setSize(getWidth(),(itempanelHeight+vgap)*filteredItems.size());
            playground.setPreferredSize(new Dimension(getWidth()-40,(itempanelHeight+vgap)*filteredItems.size()));

            if(getHeight()-130>(itempanelHeight+vgap)*filteredItems.size()) {
                scrollPane.setPreferredSize(new Dimension(getWidth()-40, (itempanelHeight + vgap+10) * filteredItems.size()));
                scrollPane.setSize(new Dimension(getWidth()-40, (itempanelHeight + vgap+10) * filteredItems.size()));
            }

            for(Item i:filteredItems) {

                final ItemJPanel filteredItemsPanel=new ItemJPanel(i, null, null);
//            filteredItemsPanel.itemsPanel=this;
                filteredItemsPanel.setSize(getWidth(),itempanelHeight);
                filteredItemsPanel.setPreferredSize(new Dimension(getWidth()-40,itempanelHeight));
                playground.add(filteredItemsPanel);
                filteredItemsPanel.clickRunnable=(()->{

                    if(itemSelectedRunnable!=null)
                    {
                        itemSelectedRunnable.setClickedItem(i);
                        itemSelectedRunnable.run();
                    }
                });

            }

            playground.setBackground(new Color(191,191,191));
            scrollPane.setBackground(new Color(191,191,191));

            repaint();
        }


}




