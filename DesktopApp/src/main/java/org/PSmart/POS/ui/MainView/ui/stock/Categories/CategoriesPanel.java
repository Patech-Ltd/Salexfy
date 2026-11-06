package org.PSmart.POS.ui.MainView.ui.stock.Categories;

import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.ui.MainView.ui.stock.Categories.popups.EditCategory;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;
import org.PSmart.POS.Repositories.ItemCategoryRepository;
import org.PSmart.POS.Utils.Components.ItemCategoryComponent.CategoryJPanel;
import org.PSmart.POS.Utils.EditItemRunnable;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.Utils.Components.PTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class CategoriesPanel extends JPanel {

    private StockPanel stockPanel;

    private JButton addNew;


    private PTextField search;


    public CategoriesPanel(StockPanel stockPanel) {
        this.stockPanel =stockPanel;

        draw();
    }

    public static EditCategory editCategory;

    private void draw() {
        setLocation(15,0);
        setSize(stockPanel.getPlayground().getSize());
        setLayout(null);
        setBackground(new Color(191,191,191));
        addNew=new JButton("+   Add New Category");
        addNew.setBackground(new Color(101, 103, 88));
        addNew.setFont(MyUtils.courierFont(20));
        addNew.setForeground(Color.WHITE);

        addNew.setLocation(getWidth()/4,10);
        addNew.setSize(getWidth()/2,35);

        addNew.addActionListener((e -> {
            if(editCategory!=null)
                editCategory.dispose();

            editCategory=new EditCategory(null,stockPanel.getDashBoard().getDbsession(),stockPanel.getDashBoard().getLoggedUser());

            editCategory.setEditCategoryRunnable(new EditItemRunnable() {
                @Override
                public void run() {
                    drawItems();
                }
            });
        }));

        add(addNew);

        search=new PTextField(new Color(52,58,64),new Color(0,0,255),"Search Category", IconsUtils.getIcon("search.png"));

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

    private void filterItems(String text) {

        try {
            if (scrollPane != null)
                remove(scrollPane);
        }catch (Exception es)
        {

        }

        List<ItemCategory> filteredcategories=new ArrayList<>();
        for(ItemCategory i:categories)
            if(i.getSearchString().contains(text.toLowerCase()))
                filteredcategories.add(i);



        GridLayout layout=new GridLayout(filteredcategories.size(),1);



        int itempanelHeight=70;
        int vgap=5;

        layout.setVgap(vgap);

        playground=new JPanel(layout);

        scrollPane=new JScrollPane(playground);

        scrollPane.setLocation(0,140);
        scrollPane.setSize(getWidth(),getHeight()-150);

        add(scrollPane);

        playground.setSize(getWidth(),(itempanelHeight+vgap)*filteredcategories.size());
        playground.setPreferredSize(new Dimension(getWidth()-40,(itempanelHeight+vgap)*filteredcategories.size()));

        if(getHeight()-130>(itempanelHeight+vgap)*filteredcategories.size()) {
            scrollPane.setPreferredSize(new Dimension(getWidth()-40, (itempanelHeight + vgap) * filteredcategories.size()));
            scrollPane.setSize(new Dimension(getWidth()-40, (itempanelHeight + vgap) * filteredcategories.size()));
        }

        for(ItemCategory i:filteredcategories) {

            CategoryJPanel itemsPanel=new CategoryJPanel(i, stockPanel.getDashBoard().getLoggedUser(),stockPanel.getDashBoard().getDbsession());

            itemsPanel.setSize(getWidth(),itempanelHeight);
            itemsPanel.setPreferredSize(new Dimension(getWidth()-40,itempanelHeight));
            playground.add(itemsPanel);

        }

        playground.setBackground(new Color(191,191,191));
        scrollPane.setBackground(new Color(191,191,191));

        repaint();
    }

    private JPanel playground;
    private JScrollPane scrollPane;

    private java.util.List<ItemCategory> categories;

    private void drawItems() {

        try {
            if (scrollPane != null)
                remove(scrollPane);
        }catch (Exception es)
        {

        }


       categories= ItemCategoryRepository.findAll();

        if(search.getText().trim().length()>0)
        {
            filterItems(search.getText().trim());
            return;
        }




        GridLayout layout=new GridLayout(categories.size(),1);



        int itempanelHeight=70;
        int vgap=5;

        layout.setVgap(vgap);

        playground=new JPanel(layout);

        scrollPane=new JScrollPane(playground);

        scrollPane.setLocation(0,140);
        scrollPane.setSize(getWidth(),getHeight()-150);

        add(scrollPane);

        playground.setSize(getWidth(),(itempanelHeight+vgap)*categories.size());
        playground.setPreferredSize(new Dimension(getWidth()-40,(itempanelHeight+vgap)*categories.size()));

        if(getHeight()-130>(itempanelHeight+vgap)*categories.size()) {
            scrollPane.setPreferredSize(new Dimension(getWidth()-40, (itempanelHeight + vgap) * categories.size()));
            scrollPane.setSize(new Dimension(getWidth()-40, (itempanelHeight + vgap) * categories.size()));
        }

        for(ItemCategory i:categories) {

            CategoryJPanel itemsPanel=new CategoryJPanel(i, stockPanel.getDashBoard().getLoggedUser(),stockPanel.getDashBoard().getDbsession());

            itemsPanel.setSize(getWidth(),itempanelHeight);
            itemsPanel.setPreferredSize(new Dimension(getWidth()-40,itempanelHeight));
            playground.add(itemsPanel);

        }

        playground.setBackground(new Color(191,191,191));
        scrollPane.setBackground(new Color(191,191,191));

        repaint();
        
    }
}
