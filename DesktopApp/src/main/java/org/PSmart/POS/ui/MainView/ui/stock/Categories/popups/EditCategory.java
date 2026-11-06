package org.PSmart.POS.ui.MainView.ui.stock.Categories.popups;

import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.ItemCategoryRepository;
import org.PSmart.POS.Utils.*;
import org.PSmart.POS.Utils.Components.ImageLabel;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.tests.AutoClosingNotification;
import org.PSmart.POS.tests.FileChooserUtils;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class EditCategory extends JFrame {

    private Session dbsession;
    private Dimension len=Toolkit.getDefaultToolkit().getScreenSize();

    private ItemCategory category;
    private User loggedUser;

    private PTextField categoryname;

    private JButton saveButton;

    private PTextField description;

    private ImageLabel imageLabel;

    private JButton uploadImage;

    private File toUpload;

    private EditItemRunnable editItemRunnable;



    public EditCategory(ItemCategory category, Session dbsession, User loggedUser)
    {
        this.category=category;
        this.loggedUser=loggedUser;
        this.dbsession=dbsession;

        draw();
        saveButton.addActionListener((e)->{
            handleSave();
        });


        repaint();
        setLocationRelativeTo(null);
    }



    private void draw() {

        setLayout(null);
        setSize(len.width*2/3,len.height-100);
        getContentPane().setBackground(Color.WHITE);
        setTitle("Add New Category");
        if(category!=null)
            setTitle("Edit Category "+category.getName());

        setVisible(true);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        categoryname=new PTextField(new Color(0,0,137),new Color(0,0,255),"Category Name",null);

        categoryname.setSize(getWidth()*3/5,70);


        categoryname.setLocation(getWidth()/20,30);

        add(categoryname);

        description = new PTextField(new Color(0,0,137),new Color(0,0,255),"Category Description",null);
        description.setSize(getWidth()*3/5,70);
        description.setLocation(getWidth()/20,150);

        add(description);




        saveButton=new JButton("SAVE");
        saveButton.setFont(MyUtils.courierFont(20));
        saveButton.setSize(300,70);
        saveButton.setBackground(Color.blue);
        saveButton.setForeground(Color.WHITE);

        saveButton.setLocation(getWidth()/2-150,230);

        add(saveButton);



        imageLabel=new ImageLabel(null,200,200);

        imageLabel.setLocation(getWidth()-210,2);
        imageLabel.setSize(200,200);

        add(imageLabel);



        uploadImage=new JButton("Choose Photo");
        uploadImage.setSize(200-10,30);
        uploadImage.setLocation(getWidth()-210,215);

        add(uploadImage);

        if(category!=null) {
            categoryname.setText(category.getName());
            description.setText(category.getDescription());
            try{
                category.getImage(new ImageResultRunnable() {
                    @Override
                    public void run() {
                        if(getBufferedImage()!=null)
                        {
                            imageLabel.setImage(getBufferedImage());
                        }
                    }
                });

            }catch (Exception es)
            {
                es.printStackTrace();
            }
        }

        uploadImage.addActionListener((e)->{

            toUpload= FileChooserUtils.selectFile();
            if(toUpload!=null) {
                    imageLabel.setImage(toUpload);
            }

        });







    }


    private void handleSave() {

        String newCategoryName = categoryname.getField().getText().trim();
        String descriptionstr = description.getField().getText();

        //check if name already exists
        String hql = "FROM ItemCategory c WHERE c.name = :name";
        ItemCategory itemCategory = dbsession.createQuery(hql, ItemCategory.class)
                .setParameter("name", newCategoryName)
                .uniqueResult();

        if(category!=null&&itemCategory!=null)
            if(category.getUid()!=null&&category.getUid()==itemCategory.getUid())
                itemCategory=null;

        if (itemCategory != null) {
            JOptionPane.showMessageDialog(this, "Item Category Already Exists", "Conflict Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

//        if (category != null&&itemCategory) {
//            JOptionPane.showMessageDialog(this, "Item Category Already Exists", "Conflict Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }

        ItemCategory tosave = category;

        if (tosave == null)
        {
            tosave = ItemCategory.builder()
                    .name(newCategoryName)
                    .Description(descriptionstr)
                    .build();
         }
         else
         {
             tosave.setName(newCategoryName);
             tosave.setDescription(descriptionstr);

         }


         try {
             if (category == null)
                 ItemCategoryRepository.save(tosave);
             else
                 ItemCategoryRepository.update(tosave);
         }catch (Exception es)
         {

         }



        if(toUpload!=null)
            tosave.setImage(toUpload);

//        JOptionPane.showMessageDialog(this,"Saved Successfully");
        AutoClosingNotification.sendNotification("Saved Successfully");
        dispose();
        if(editItemRunnable !=null)
            editItemRunnable.run();



    }


    public void setEditCategoryRunnable(EditItemRunnable editItemRunnable)
    {
        this.editItemRunnable = editItemRunnable;
    }


}
