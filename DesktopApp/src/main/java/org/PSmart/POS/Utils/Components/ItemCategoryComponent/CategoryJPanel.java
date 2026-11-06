package org.PSmart.POS.Utils.Components.ItemCategoryComponent;

import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.ui.MainView.ui.stock.Categories.popups.EditCategory;
import org.PSmart.POS.Repositories.ItemCategoryRepository;
import org.PSmart.POS.Utils.Components.ImageLabel;
import org.PSmart.POS.Utils.EditItemRunnable;
import org.PSmart.POS.Utils.ImageResultRunnable;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static org.PSmart.POS.ui.MainView.ui.stock.Categories.CategoriesPanel.editCategory;


public class CategoryJPanel extends JPanel {

    private ItemCategory itemCategory;
    private JLabel categoryname;

    private JLabel description;

    private ImageLabel imageLabel;


    private User loggedUser;

    private Session dbsession;

    public CategoryJPanel(ItemCategory itemCategory, User loggedUser, Session dbsession) {
        this.itemCategory = itemCategory;
        this.loggedUser = loggedUser;
        this.dbsession = dbsession;

        feedData();
    }

    private void feedData() {
        try{

            removeAll();
            repaint();
            for(MouseListener i:getMouseListeners())
                removeMouseListener(i);

        }catch (Exception ed)
        {

        }
        if(itemCategory==null)
        {
            setSize(getWidth(),0);
            setPreferredSize(new Dimension(getWidth(),0));

            return;
        }

        categoryname=new JLabel(itemCategory.getName());
        description=new JLabel(itemCategory.getDescription());

        imageLabel=new ImageLabel(null,60,60);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if(editCategory!=null)
                    editCategory.dispose();

                editCategory=new EditCategory(itemCategory,dbsession,loggedUser);


                editCategory.setEditCategoryRunnable(new EditItemRunnable() {
                    @Override
                    public void run() {
                        itemCategory= ItemCategoryRepository.findById(itemCategory.getUid());
                        feedData();
                    }
                });


                editCategory.setVisible(true);
                editCategory.repaint();
            }
        });
        setBackground(new Color(255,255,255));
        draw();

        itemCategory.getImage(new ImageResultRunnable() {
            @Override
            public void run() {
                imageLabel.setImage(getBufferedImage());
            }
        });
    }

    private JPanel playground;

    private JScrollPane scrollPane;
    private void draw() {

        setLayout(new BorderLayout());
        add(imageLabel,BorderLayout.WEST);

        playground=new JPanel(new GridLayout(2,2));
//        playground.setLayout(new GridLayout(2,2));

        add(playground,BorderLayout.CENTER);
        playground.add(categoryname);
        playground.add(description);

        categoryname.setForeground(Color.BLUE);

    }
}
