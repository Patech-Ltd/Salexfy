package org.PSmart.POS.ui.MainView.ui.stock.Items.popups;

import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Repositories.ItemCategoryRepository;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Repositories.SubScaleRepository;
import org.PSmart.POS.Repositories.WholeScalesRepository;
import org.PSmart.POS.Utils.*;
import org.PSmart.POS.Utils.Components.*;
import org.PSmart.POS.tests.AutoClosingNotification;
import org.PSmart.POS.tests.FileChooserUtils;
import org.hibernate.Session;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.io.File;

public class EditItem extends JFrame {

    private Session dbsession;
    private Dimension len=Toolkit.getDefaultToolkit().getScreenSize();

    private Item item;
    private User loggedUser;

    private PTextField itemname;

    private JButton saveButton;

    private PTextField description;

    private ImageLabel imageLabel;

    private JButton uploadImage;

    private File toUpload;

    private EditItemRunnable editItemRunnable;


    private ScalesPanel[] wholeScales;

    private ScalesPanel[] subSCales;
    private Runnable deleteAction;
    private Runnable saveAction;


    public EditItem(Item item, Session dbsession, User loggedUser)
    {
        this.item = item;
        this.loggedUser=loggedUser;
        this.dbsession=dbsession;

        draw();


        configValues();
        repaint();
        setLocationRelativeTo(null);
    }




    private void draw() {

        setLayout(null);
        setSize(len.width*2/3,len.height-100);
        getContentPane().setBackground(Color.WHITE);
        setTitle("Add New Item");
        if(item !=null)
            setTitle("Edit Item "+ item.getName());

        setVisible(true);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        itemname =new PTextField(new Color(0,0,137),
                new Color(0,0,255),"Item Name",null);

        itemname.setSize(getWidth()*3/5,70);


        itemname.setLocation(getWidth()/20,30);

        add(itemname);

        description = new PTextField(new Color(0,0,137),new Color(0,0,255),"Item Description",null);
        description.setSize(getWidth()*3/5,70);
        description.setLocation(getWidth()/20,135);

        add(description);



        imageLabel=new ImageLabel(null,200,200);

        imageLabel.setLocation(getWidth()-210,2);
        imageLabel.setSize(200,200);

        add(imageLabel);



        uploadImage=new JButton("Choose Photo");
        uploadImage.setSize(200-10,30);
        uploadImage.setLocation(getWidth()-210,215);

        add(uploadImage);

        if(item !=null) {
            itemname.setText(item.getName());
            description.setText(item.getDescription());
            try{
                item.getImage(new ImageResultRunnable() {
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

        int baseh=150;
        int hspacing=80;


        baseh+=hspacing;
        barCode=new PTextField(new Color(0,0,137),new Color(0,0,255),"Item BarCode",null);
        barCode.setSize(getWidth()/2-getWidth()/10,baseh);
        barCode.setLocation(getWidth()/20,220);

        add(barCode);



        category=new PCategorySelector(new Color(0,0,137),new Color(0,0,255),"Item Category",null);
        this.add(category);
        category.setLocation(getWidth()/20+getWidth()/2,baseh);
        category.setSize(getWidth()/2-getWidth()/10,70);


        baseh+=hspacing;


        unitBuyingPrice=new PTextField(new Color(0,0,137),new Color(0,0,255),"Unit Buying Price",null);
        this.add(unitBuyingPrice);
        unitBuyingPrice.setSize(getWidth()/2-getWidth()/10,70);
        unitBuyingPrice.setLocation(getWidth()/20,baseh);


        unitSellingPrice=new PTextField(new Color(0,0,137),new Color(0,0,255),"Unit Selling Price",null);
        this.add(unitSellingPrice);
        unitSellingPrice.setSize(getWidth()/2-getWidth()/10,70);
        unitSellingPrice.setLocation(getWidth()/20+getWidth()/2,baseh);

        baseh+=hspacing;

        reorderLevel=new PTextField(new Color(0,0,137),new Color(0,0,255),"Re-order Level (Low stock)",null);
        this.add(reorderLevel);
        reorderLevel.setSize(getWidth()/2-getWidth()/10,70);
        reorderLevel.setLocation(getWidth()/20,baseh);

        expiryDate=new PTextField(new Color(0,0,137),new Color(0,0,255),"Expiry Date (dd-MM-yyyy, optional)",null);
        this.add(expiryDate);
        expiryDate.setSize(getWidth()/2-getWidth()/10,70);
        expiryDate.setLocation(getWidth()/20+getWidth()/2,baseh);



        supplierEmail=new PTextField(new Color(0,0,137),new Color(0,0,255),"Supplier Email",null);
        this.add(supplierEmail);
        supplierEmail.setSize(getWidth()/2-getWidth()/10,70);
        supplierEmail.setLocation(getWidth()/20+getWidth()/2,baseh);

        baseh+=hspacing-10;


        setasDoubles(unitBuyingPrice,unitSellingPrice,reorderLevel);


        JLabel wholeScaleLabel=new JLabel("Whole-Scales",JLabel.CENTER);
        this.add(wholeScaleLabel);
        wholeScaleLabel.setSize(getWidth()/2-getWidth()/10,20);
        wholeScaleLabel.setLocation(getWidth()/20,baseh);

        JLabel subScaleLabel=new JLabel("Sub-Scales",JLabel.CENTER);
        this.add(subScaleLabel);
        subScaleLabel.setSize(getWidth()/2-getWidth()/10,20);
        subScaleLabel.setLocation(getWidth()/2+getWidth()/20,baseh);
        subScaleLabel.setBackground(null);


        hspacing=30;
        baseh+=hspacing;

        ScalesLabel whscale=new ScalesLabel("Items","WholeScale Name","price_per_scale");


        ScalesLabel sbscale=new ScalesLabel("divisions","SubScale Name","price_per_scale");

        whscale.setSize(getWidth()/2-getWidth()/10,30);
        sbscale.setSize(whscale.getSize());

        whscale.setLocation(getWidth()/20,baseh);
        sbscale.setLocation(getWidth()/2+getWidth()/20,baseh);

        add(whscale);
        add(sbscale);

        baseh+=hspacing;

        wholeScales=new ScalesPanel[scalesLevels];
        subSCales=new ScalesPanel[scalesLevels];

        for(int i=0;i<scalesLevels;i++)
        {
            wholeScales[i]=new ScalesPanel();
            subSCales[i]=new ScalesPanel();

            wholeScales[i].setSize(whscale.getSize());
            subSCales[i].setSize(whscale.getSize());

            wholeScales[i].setLocation(getWidth()/20,baseh);
            subSCales[i].setLocation(getWidth()/20+getWidth()/2,baseh);
            baseh+=hspacing;
            add(wholeScales[i]);
            add(subSCales[i]);
        }

        baseh+=35;

        delete=MyUtils.createButton("Delete Item",Color.RED);
        delete.setSize(250,50);
        delete.setLocation(getWidth()/4-125,baseh);
        delete.setForeground(Color.WHITE);
        delete.setFont(MyUtils.courierFont(25));

        save=MyUtils.createButton("Save Item",Color.BLUE);
        save.setSize(delete.getSize());
        save.setLocation(getWidth()-getWidth()/4-125,baseh);
        save.setFont(MyUtils.courierFont(25));
        save.setForeground(Color.WHITE);

        add(delete);
        add(save);



        baseh+=170;
//        int width=getWidth();
//
//            getContentPane().setSize(len.width*2/3,baseh);
        setSize(len.width*2/3,baseh);


        repaint();

    }

    private int scalesLevels=3;

    private JButton delete;
    private JButton save;


    private void setasDoubles(PTextField... fields) {
        for (PTextField p:fields)
        {
            JTextField ff=p.getField();
//            ff.getDocument().s
            AbstractDocument document = (AbstractDocument) ff.getDocument();
            document.setDocumentFilter(new DecimalDocumentFilter());

        }
    }

    private java.util.List<ItemCategory> categories= ItemCategoryRepository.findAll();

  private PTextField barCode;//=new PTextField(new Color(0,0,137),new Color(0,0,255),"Item Description",null);

    private PCategorySelector category;

    private PTextField unitBuyingPrice;

    private PTextField unitSellingPrice;

    private PTextField reorderLevel;

    private PTextField expiryDate;

    private PTextField supplierEmail;





    public void setEditItemRunnable(EditItemRunnable editItemRunnable)
    {
        this.editItemRunnable = editItemRunnable;
    }

    private void configValues() {
        if(item==null)
        {
            save.setText("Add Item");
            delete.setEnabled(false);
        }
        else{
            itemname.setText(item.getName());
            description.setText(item.getDescription());
            barCode.setText(item.getBarcode());
            {
                if(item.getCategory()!=null)
                    category.getICComboBox().setSelectedItem(item.getCategory());
                else
                    category.getICComboBox().setSelectedIndex(0);
            }
            unitBuyingPrice.setText(item.getBuyingPrice().toString());
            unitSellingPrice.setText(item.getSellingPrice().toString());
            reorderLevel.setText(item.getReorderLevel().toString());
            supplierEmail.setText(item.getSupplier());

            if (item.getExpiryDate() != null) {
                expiryDate.setText(new java.text.SimpleDateFormat("dd-MM-yyyy").format(new java.util.Date(item.getExpiryDate())));
            }

            WholeScale[] mywholeScales=item.getWholeScales();
            if(mywholeScales!=null)
            {
                for(int i=0;i<mywholeScales.length&&i<scalesLevels;i++)
                {
                    try {
                        wholeScales[i].getItemsField().setText(mywholeScales[i].getItems().toString());
                        wholeScales[i].getNameField().setText(mywholeScales[i].getName());
                        wholeScales[i].getPriceField().setText(mywholeScales[i].getUnitPrice().toString());

                    }catch (Exception es)
                    {

                    }
                }
            }

            SubScale[] mysubScales=item.getSubScales();
            if(mysubScales!=null)
            {
                for(int i=0;i<mysubScales.length&&i<scalesLevels;i++)
                {
                    try {
                        subSCales[i].getItemsField().setText(mysubScales[i].getDivisions().toString());
                        subSCales[i].getNameField().setText(mysubScales[i].getName());
                        subSCales[i].getPriceField().setText(mysubScales[i].getUnitPrice().toString());

                    }catch (Exception es)
                    {

                    }
                }
            }




            delete.addActionListener(e -> {
                if(!org.PSmart.POS.Utils.AuthorizationUtils.authorizeAction(this, loggedUser,
                        org.PSmart.POS.Entities.RoleType.STOCK, "Delete Item")) return;

                int out=JOptionPane.showConfirmDialog(this,"Are you sure you want to delete this item ?","Confirm Delete",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
                if(out==JOptionPane.YES_OPTION)
                {
                    if(item!=null)
                        ItemRepository.delete(item);

                    if(deleteAction!=null)
                        deleteAction.run();
                }
            });


        }
        save.addActionListener(e -> {
            performSave();
        });
    }

    private void performSave() {

        String itmname=itemname.getText().trim();
        String barcodeno=barCode.getText().trim();

        Item toSave=item;
        if(toSave==null)
            toSave=new Item();



        Item existing=ItemRepository.findByName(itmname);

        System.out.println("Saving");

        if(existing!=null&&!(item!=null&&item.getUid()!=null&&item.getUid().equals(existing.getUid())))
        {
            JOptionPane.showMessageDialog(this, "Item Name Already Exists", "Conflict Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if(barcodeno.length()>0) {

            existing = ItemRepository.findByBarCode(barcodeno);

            if (existing != null && !(item!=null&&item.getUid() != null && item.getUid().equals(existing.getUid()))) {
                JOptionPane.showMessageDialog(this, "Barcode Already Exists on item : " + existing.getName(), "Conflict Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }


        toSave.setName(itmname);
        toSave.setDescription(description.getText());
        if(barcodeno.length()>0)
            toSave.setBarcode(barcodeno);

        try {
            toSave.setCategory(ItemCategoryRepository.findById(((ItemCategory) category.getICComboBox().getSelectedItem()).getUid()));
        }catch (Exception es)
        {

        }

        toSave.setBuyingPrice(MyUtils.getDouble(unitBuyingPrice.getText()));
        toSave.setSellingPrice(MyUtils.getDouble(unitSellingPrice.getText()));
        toSave.setReorderLevel(MyUtils.getDouble(reorderLevel.getText()));
        toSave.setSupplier(supplierEmail.getText());
        toSave.setExpiryDate(parseExpiry(expiryDate.getText().trim()));





        if(item==null)
            ItemRepository.save(toSave);
        else
            ItemRepository.update(toSave);


        //deal with scales;

        {
            //wholescales
            if(toSave.getWholeScales()==null||toSave.getWholeScales().length!=scalesLevels)
                toSave.setWholeScales(new WholeScale[scalesLevels]);



            //subscales
            if(toSave.getSubScales()==null||toSave.getSubScales().length!=scalesLevels)
                toSave.setSubScales(new SubScale[scalesLevels]);


            //fetch WHolescales
            {
                for (int i = 0; i < scalesLevels; i++) {
                    Double itemsc = MyUtils.getDouble(wholeScales[i].getItemsField().getText());
                    String namec = wholeScales[i].getNameField().getText().trim();
                    Double pricec = MyUtils.getDouble(wholeScales[i].getPriceField().getText());

                    if (itemsc > 0 && !namec.isEmpty()) {
                        System.out.println("have items");
                        WholeScale nww = WholeScale.builder()
                                .items(itemsc)
                                .name(namec)
                                .unitPrice(pricec)
                                .itemId(toSave.getUid())
                                .build();

                        if (nww.isDiffent(toSave.getWholeScales()[i])) {
                            WholeScalesRepository.save(nww);
                            toSave.getWholeScales()[i] = nww;

                        }
                    }

                }
            }

            //fetch subscales
            {
                for (int i = 0; i < scalesLevels; i++) {
                    Double divs = MyUtils.getDouble(subSCales[i].getItemsField().getText());
                    String namec =subSCales[i].getNameField().getText().trim();
                    Double pricec = MyUtils.getDouble(subSCales[i].getPriceField().getText());

                    if ( divs> 0 && !namec.isEmpty()) {
                        System.out.println("have divisions");
                        SubScale nww = SubScale.builder()
                                .divisions(divs)
                                .name(namec)
                                .unitPrice(pricec)
                                .itemId(toSave.getUid())
                                .build();

                        if (nww.isDiffent(toSave.getSubScales()[i])) {
                            SubScaleRepository.save(nww);
                            toSave.getSubScales()[i] = nww;

                        }
                    }

                }
            }

            System.out.println(toSave);

            ItemRepository.update(toSave);
        }




        if(toUpload!=null)
            toSave.setImage(toUpload);

        AutoClosingNotification.sendNotification("Saved Successfully");
        this.dispose();


        if(saveAction!=null)
            new Thread(saveAction).start();

    }


    public void setDeleteAction(Runnable runnable)
    {
        this.deleteAction=runnable;
    }

    public void setSaveAction(Runnable runnable)
    {
        this.saveAction=runnable;
    }

    /** Parse "dd-MM-yyyy" (or dd/MM/yyyy, dd-MMM-yyyy, yyyy-MM-dd) to epoch millis; null if blank/invalid. */
    private Long parseExpiry(String text) {
        if (text == null || text.isEmpty()) return null;
        String[] fmts = {"dd-MM-yyyy", "dd/MM/yyyy", "dd-MMM-yyyy", "yyyy-MM-dd"};
        for (String f : fmts) {
            try {
                java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(f);
                fmt.setLenient(false);
                return fmt.parse(text).getTime();
            } catch (Exception ignored) {}
        }
        return null;
    }

}
