package org.PSmart.POS.Utils.Components.GridSearch.ItemComponents;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.Repositories.ItemCategoryRepository;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.ImageResultRunnable;
import org.PSmart.POS.Utils.MyUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

public class GridtemsSearch extends JFrame {

    private PTextField search;

    private JPanel playground;

    private java.util.List<Item> items;

    private java.util.List<ItemCategory> categories;

    private HashMap<String, List<Item>> categoriesItems=new HashMap<>();


    private HashMap<UUID, GridItemJpanel> selectedCarts=new HashMap<UUID, GridItemJpanel>();



    public GridtemsSearch()
    {
        super();
        items= ItemRepository.findAllAsc();
        categories = ItemCategoryRepository.findAllAsc();
//        while(categories==null) {
//            try {
//                categories = ItemCategoryRepository.findAllAsc();
//            } catch (Exception ess) {
//                ess.printStackTrace();
//            }
//        }
        categoriesMenu=new JButton[categories.size()+1];
        arrangeInCategories();
        draw();
        repaint();
        repaint();
        getContentPane().repaint();
        setVisible(true);
    }

    private void arrangeInCategories() {
        categoriesItems.put("All",items);
        for(Item i:items)
        {
            if(i.getCategory()==null)
                continue;

            String nm=i.getCategory().getName();

            List<Item> found=categoriesItems.get(nm);
            if(found==null) {
                found = new ArrayList<Item>();
                categoriesItems.put(nm,found);
            }
            found.add(i);
        }
    }

    public static Dimension len=Toolkit.getDefaultToolkit().getScreenSize();

    private List<GridItemJpanel> selectedCartsItems=new LinkedList<>();



    public GridItemSelectedRunnable itemSelectedRunnable;

    private void draw() {
        getContentPane().setLayout(new BorderLayout());

        Point p=MouseInfo.getPointerInfo().getLocation();

        if(p.y>len.height/2+50) {
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
                new Thread(()->{
                    filterItems(search.getText());
                }).start();

            }
        });


        addCategoriesPannel();



        submit=new JButton("Submit");
        submit.setBackground(Color.BLUE);
        submit.setForeground(Color.WHITE);

        if(doInverse)
            add(submit,BorderLayout.NORTH);
        else
            add(submit,BorderLayout.SOUTH);


        repaint();

//        submit.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//
//            }
//        });


    }

    public void setSubmitAction(ActionListener action)
    {
        submit.addActionListener(action);
    }

    public List<GridItemJpanel> getSelectedCartsItems() {
        return selectedCartsItems;
    }

    public Boolean doInverse=false;
    public JButton getSubmit() {
        return submit;
    }

    private JButton submit;

    private JPanel categoriesPanel;

    private JScrollPane categoriesScrollPane;

    private void addCategoriesPannel() {

        categoriesPanel=new JPanel(new GridLayout(categories.size()+2,1));
        categoriesScrollPane=new JScrollPane(categoriesPanel);



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
            ItemCategory cc=categories.get(i-1);
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

        categoriesPanel.repaint();
        categoriesScrollPane.repaint();
        add(categoriesScrollPane,BorderLayout.WEST);

    }


    public void setActiveTab(int tab)
    {
        for(int i=0;i<categoriesMenu.length;i++)
        {
            if(i==tab)
                categoriesMenu[i].setBackground(new Color(25, 118, 211));
            else
                try {
                    categoriesMenu[i].setBackground(new Color(0, 0, 139));
                }catch (Exception esd)
                {
                    System.out.println("error "+esd+" on "+i);
//                    esd.printStackTrace();
                }

        }

    }




    private JButton[] categoriesMenu;

    private String selectedCategory="All";


    private JScrollPane scrollPane;










    Dimension prevheight=null;
    Point prevLoc=null;



    public void filterItems(String str) {


        try {
            if (scrollPane != null) {
                prevheight=scrollPane.getSize();
                prevLoc=scrollPane.getLocation();
                remove(scrollPane);
            }
        }catch (Exception es)
        {
            es.printStackTrace();

        }

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
            currentList=new ArrayList<>();

        java.util.List<Item> filteredItems=new ArrayList<>();
        if(str.isEmpty())
            filteredItems.addAll(currentList);
        else {
            for (Item i : currentList)
                if (i.getSearchString().contains(str.toLowerCase()))
                    filteredItems.add(i);
        }

        int cols=getWidth()/175;
        int rows=filteredItems.size()/cols+1;
        GridLayout layout=new GridLayout(rows,cols);

        layout.setHgap(10);
        layout.setVgap(10);



//        playground=new JPanel(layout);
//
//        scrollPane=new JScrollPane(playground);
//
////        scrollPane.setLocation(0,140);
////        scrollPane.setSize(getWidth(),getHeight()-150);
//
//        add(scrollPane,BorderLayout.CENTER);
//
////        playground.setSize(getWidth(),(itempanelHeight+vgap)*filteredItems.size());
////        playground.setPreferredSize(new Dimension(getWidth()-40,(itempanelHeight+vgap)*filteredItems.size()));
////

//        scrollPane.setSize(getWidth()-categoriesScrollPane.getWidth(),getHeight()-search.getHeight());

        if(playground!=null)
            playground.removeAll();

        playground=new JPanel(layout);

        scrollPane=new JScrollPane(playground);

        if(prevheight!=null) {
            scrollPane.setLocation(categoriesScrollPane.getWidth(),search.getHeight());
            scrollPane.setSize(getWidth()-categoriesScrollPane.getWidth(), getHeight()-search.getHeight());

        }

        add(scrollPane,BorderLayout.CENTER);
        playground.repaint();
        repaint();
        scrollPane.repaint();

//        playground.getSize().height=rows*90+10;
//        playground.getPreferredSize().height=rows*90+10;

        int playgroundHeight=rows*110+10;

        playground.setSize((cols)*(170),playgroundHeight);
        playground.setPreferredSize(new Dimension((cols)*(170),playgroundHeight));

        //adjust size;
        if(getHeight()-100>playgroundHeight)
        {
            scrollPane.setSize((cols)*(170)+10,playgroundHeight+20);
            scrollPane.setPreferredSize(new Dimension((cols)*(170)+10,playgroundHeight+20));
        }

        scrollPane.invalidate();
        scrollPane.revalidate();
        playground.invalidate();
        playground.revalidate();


        for(Item i:filteredItems) {

            GridItemJpanel filteredItemsPanel = selectedCarts.get(i.getUid());
            if (filteredItemsPanel == null)
            {
//            filteredItemsPanel.itemsPanel=this;
                filteredItemsPanel=new GridItemJpanel(i, null, null);
                selectedCarts.put(i.getUid(),filteredItemsPanel);
                GridItemJpanel finalFilteredItemsPanel = filteredItemsPanel;
                filteredItemsPanel.actionRunnable=(()->{
                    if(!selectedCartsItems.contains(finalFilteredItemsPanel))
                        selectedCartsItems.add(finalFilteredItemsPanel);
                });

            }
            playground.add(filteredItemsPanel);


        }

        playground.setBackground(new Color(191,191,191));
        scrollPane.setBackground(new Color(191,191,191));

        repaint();
        scrollPane.repaint();
        playground.repaint();


        invalidate();
        revalidate();
    }

    public HashMap<UUID, GridItemJpanel> getSelectedCarts() {
        return selectedCarts;
    }


}




