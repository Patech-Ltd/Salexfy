package org.PSmart.POS.Utils.Components.cart;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Entities.StockFeedItem;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.ItemRepository;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridItemSelectedRunnable;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridtemsSearch;
import org.PSmart.POS.Utils.Components.ImageLabel;
import org.PSmart.POS.Utils.Components.ImageLabels.IconLabel;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.Components.Runnables.ItemSelectedRunnable;
import org.PSmart.POS.Utils.Components.cart.feedDataItemReceive.CartLineFeedDataReceiver;
import org.PSmart.POS.Utils.Components.listSearch.SearchItemListPrompt;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.tests.AutoClosingNotification;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;

public class CartLine extends JPanel {

    public GridItemSelectedRunnable gridItemSelectedRunnable;
    private PTextField barcode;

    private IconLabel gridSearch;

    private IconLabel close;

    private JPanel leftMost;

    private JPanel rightMost;

    private StockFeedItem stockFeedItem;

    private Session dbsession;

    private User loggedUser;

    private JPanel playground;

    public CartLine(StockFeedItem stockFeedItem, Session dbsession, User loggedUser) {
        this.stockFeedItem = stockFeedItem;
        this.dbsession = dbsession;
        this.loggedUser = loggedUser;

        draw();


        barcodeListener();

        searchPrompt();

        launchGridSearch();


//        new Thread(()->{
////            try {
////                Thread.sleep(1000);
////            } catch (InterruptedException e) {
////                throw new RuntimeException(e);
////            }
////            drawData();
//
////
////        try {
////            SwingUtilities.invokeAndWait(this::drawData);
////        } catch (InterruptedException e) {
////            throw new RuntimeException(e);
////        } catch (InvocationTargetException e) {
////            throw new RuntimeException(e);
////        }
//        }).start();
////        setStockFeedItem(stockFeedItem);

//
//        new Thread(()->{
//            if(stockFeedItem!=null)
//            {
//                while(isOpaque()&&!hasFocus())
//                {
//                    drawData();
//                    System.out.println("I have to draw You know "+stockFeedItem.getItem().getName());
//                    try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//
//                }
//
//            }
//        }).start();

        drawData();
//
    }




    private void draw() {

        setLayout(new BorderLayout());
        barcode=new PTextField(new Color(52,58,64),Color.BLUE,"BarCode", IconsUtils.getIcon("search.png"));
        gridSearch=new IconLabel(null,30,30);
        gridSearch.setImage(IconsUtils.getIcon("grid.png"));

        close=new IconLabel(null,30,30);
        close.setImage(IconsUtils.getIcon("delete.png"));

        leftMost=new JPanel();
        leftMost.setLayout(null);
        int widthleftmost=getWidth()/4;
        if(widthleftmost==0)
            widthleftmost=200;

        leftMost.setSize(new Dimension(widthleftmost+10,35));
        leftMost.setPreferredSize(new Dimension(widthleftmost+10,35));

        barcode.setSize(widthleftmost-40-10,30);

        barcode.setLocation(10,0);
        leftMost.add(barcode);

        gridSearch.setSize(30,30);
        gridSearch.setLocation(barcode.getWidth()+25,20);
        leftMost.add(gridSearch);


        rightMost=new JPanel();
        rightMost.setLayout(null);
        rightMost.setSize(55,35);
        rightMost.setPreferredSize(new Dimension(55,35));
        rightMost.add(close);



//        close.setSize(30,30);
//        close.setPreferredSize(new Dimension(30,30));
        close.setLocation(0,20);

        rightMost.repaint();


        add(leftMost,BorderLayout.WEST);

        add(rightMost,BorderLayout.EAST);

        playground=new JPanel();
        playground.setLayout(new GridLayout(1,1));
        playground.setPreferredSize(new Dimension(getWidth()-rightMost.getWidth()-leftMost.getWidth()-10,35));
        add(playground,BorderLayout.CENTER);
        repaint();


        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);

                System.out.println("closing");


                cartLineFeedDataReceiver=null;
                stockFeedItem=null;
                drawData();


            }
        });


    }

    private CartLineFeedDataReceiver cartLineFeedDataReceiver;

    public void drawData() {
        if(stockFeedItem!=null) {
            barcode.setText(stockFeedItem.getItem().getBarcode());

            cartLineFeedDataReceiver=new CartLineFeedDataReceiver(stockFeedItem,dbsession,loggedUser);
            try{
                playground.removeAll();
            }catch (Exception es)
            {

            }
            cartLineFeedDataReceiver.setSize(playground.getWidth(),50);
//            cartLineFeedDataReceiver.setBackground(Color.BLUE);

            playground.add(cartLineFeedDataReceiver);
            cartLineFeedDataReceiver.drawSize();
            playground.repaint();
        }
        else {
            barcode.setText("");
            try{
                playground.removeAll();
            }catch (Exception es)
            {

            }
            playground.repaint();
        }


    }

    private Runnable successStockFeed;

    public void setSuccessStockFeed(Runnable successStockFeed) {
        this.successStockFeed = successStockFeed;
    }

    public Runnable getSuccessStockFeed()
    {
        return successStockFeed;
    }

    public void setStockFeedItem(StockFeedItem stockFeedItem)
    {

        this.stockFeedItem=stockFeedItem;
        drawData();
        if(stockFeedItem!=null&&successStockFeed!=null)
            successStockFeed.run();

    }


    private void barcodeListener() {
        barcode.getField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if(e.getKeyCode()==KeyEvent.VK_ENTER)
                {
                    if(!barcode.getText().trim().isEmpty())
                    {
                        String code=barcode.getText().trim();
                        Item newItem= ItemRepository.findByBarCode(code);
                        if(newItem!=null)
                        {
                            StockFeedItem nn=StockFeedItem.builder()
                                    .item(newItem)
                                    .build();

                            setStockFeedItem(nn);
                        }
                        else
                        {
                            AutoClosingNotification.sendNotification("Code Not Found");
                        }

                    }
                }
            }
        });
    }



    private SearchItemListPrompt searchItemListPrompt;

    private GridtemsSearch gridtemsSearch;
    private void searchPrompt() {

        barcode.iconAction(()->{
            if(searchItemListPrompt!=null)
                searchItemListPrompt.dispose();

            searchItemListPrompt=new SearchItemListPrompt();
            searchItemListPrompt.itemSelectedRunnable=new ItemSelectedRunnable() {
                @Override
                public void run() {

                    searchItemListPrompt.dispose();
                    StockFeedItem nn=StockFeedItem.builder()
                            .item(getClickedItem())
                            .build();

                    setStockFeedItem(nn);
                }
            };
        });


    }


    public StockFeedItem getStockFeedItem() {
        return stockFeedItem;
    }


    private void launchGridSearch() {


        gridSearch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);



                    if (gridtemsSearch != null)
                        gridtemsSearch.dispose();

                    gridtemsSearch = new GridtemsSearch();
//                    gridtemsSearch.itemSelectedRunnable = new ItemSelectedRunnable() {
//                        @Override
//                        public void run() {
//
//                            StockFeedItem nn = StockFeedItem.builder()
//                                    .item(getClickedItem())
//                                    .build();
//
//                            setStockFeedItem(nn);
//                        }
//                    };

                gridtemsSearch.setSubmitAction(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        gridtemsSearch.dispose();
                        gridItemSelectedRunnable.setGridItemJpanels(gridtemsSearch.getSelectedCartsItems());
                        gridItemSelectedRunnable.run();
//                        new Thread().start();
                    }
                });




            }
        });

    }

    public CartLineFeedDataReceiver getCartLineFeedDataReceiver() {
        return cartLineFeedDataReceiver;
    }

    @Override
    public void grabFocus() {
        super.grabFocus();
        barcode.grabFocus();
    }
}
