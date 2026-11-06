package org.PSmart.POS.ui.MainView.ui.stock.stockfeed.popup;

import org.PSmart.POS.Entities.StockFeed;
import org.PSmart.POS.Entities.StockFeedItem;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridItemJpanel;
import org.PSmart.POS.Utils.Components.GridSearch.ItemComponents.GridItemSelectedRunnable;
import org.PSmart.POS.Utils.Components.cart.CartLine;
import org.PSmart.POS.Utils.MyUtils;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class FeedStockFrame extends JFrame {


    private StockFeed stockFeed;

    private Session dbsession;

    private User loggedUser;



    public FeedStockFrame(StockFeed feedStock, Session dbsession, User loggedUser)
    {
        this.stockFeed=feedStock;
        this.dbsession=dbsession;
        this.loggedUser=loggedUser;

        draw();
       configure();

    }



    private static Dimension len=Toolkit.getDefaultToolkit().getScreenSize();

    private JPanel playground;

    private void draw() {
        setSize(len.width*3/4,len.height-150);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        setTitle("Feed Stock");
        setLayout(null);

        JLabel ll=new JLabel("Feed Stock",JLabel.CENTER);
        if(stockFeed==null)
            ll.setSize(getWidth(),30);
        else {
            ll.setSize(getWidth() / 2, 30);
            JLabel sl=new JLabel("# "+stockFeed.getRec(),JLabel.CENTER);
            sl.setFont(MyUtils.courierFont(18));
            sl.setSize(ll.getSize());
            sl.setLocation(getWidth()/2,1);
            add(sl);

        }
        ll.setLocation(0,1);
        ll.setFont(MyUtils.courierFont(20));
        add(ll);

        addLabel("Select Items",getWidth()/20,getWidth()/6);
        addLabel("Item Name",getWidth()/5,getWidth()/5);

        int dmsize=getWidth()/10-5;
        addLabel("quantity",getWidth()/5+getWidth()/5+20,dmsize);
        addLabel("Buying Price",getWidth()/5+getWidth()/5+dmsize+20,dmsize);
        addLabel("Selling Price",getWidth()/5+getWidth()/5+dmsize*2,dmsize);

        addLabel("Expiry Date",getWidth()-getWidth()/5-35,getWidth()/6);


//        JLabel st=new JLabel("Select Items");
//        st.setSize(ll.getSize());
//        st.setLocation(getWidth()/20,30);
//        add(st);


        playground=new JPanel();
        playground.setSize(getWidth()-getWidth()/10,getHeight()-80-80);
        playground.setLocation(getWidth()/20,50);
        add(playground);








        int baseh=getHeight()-100;

        delete=MyUtils.createButton("Delete Receipt",Color.RED);
        delete.setSize(250,50);
        delete.setLocation(getWidth()/4-125,baseh);
        delete.setForeground(Color.WHITE);
        delete.setFont(MyUtils.courierFont(25));

        save=MyUtils.createButton("SUBMIT",Color.BLUE);
        save.setSize(delete.getSize());
        save.setLocation(getWidth()-getWidth()/4-125,baseh);
        save.setFont(MyUtils.courierFont(25));
        save.setForeground(Color.WHITE);

        add(delete);
        add(save);


    }

    private void addLabel(String str, int position, int width) {
        JLabel st=new JLabel(str,JLabel.CENTER);
        st.setSize(width,20);
        st.setLocation(position,30);
        add(st);
    }

    private JButton delete;
    private JButton save;


    private JPanel listPanel;

    private JScrollPane listScroll;


    private void configure() {

        int cartHeight=70;
        int spaceHeight=10;

        int totHeight=cartHeight+spaceHeight;

        int numItems=15;

        GridLayout layout=new GridLayout(numItems,1);
        layout.setVgap(spaceHeight);
        playground.setLayout(new GridLayout(1,1));

        playground.setBackground(new Color(191,191,191));



        listPanel=new JPanel(layout);
        listPanel.setSize(playground.getWidth(),totHeight*numItems);
        listPanel.setPreferredSize(new Dimension(playground.getWidth(),totHeight*numItems));
//        listPanel.setBackground(new Color(191,191,191));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(null);
        playground.setBorder(null);
        listPanel.repaint();
        playground.repaint();

        listScroll=new JScrollPane(listPanel);

        listScroll.setBorder(null);

        JScrollPane scrollPane=new JScrollPane(listScroll);

        playground.add(scrollPane);
        scrollPane.setBorder(null);



//        CartLine first=new CartLine(null,dbsession,loggedUser);
//        listPanel.add(first);
//        listPanel.add(new CartLine(null,dbsession,loggedUser));
//        listPanel.add(new CartLine(null,dbsession,loggedUser));
//
//        listPanel.add(new CartLine(null,dbsession,loggedUser));
//        listPanel.add(new CartLine(null,dbsession,loggedUser));
//        listPanel.add(new CartLine(null,dbsession,loggedUser));


        ensureEmptyCartLine();




    }

    private void ensureEmptyCartLine() {

//        if(pendingStoreFeeds!=null&&!pendingStoreFeeds.isEmpty())
//        {
//            Queue<StockFeedItem> mines=pendingStoreFeeds;
//            pendingStoreFeeds=null;
//            System.out.println("polling when "+mines.size());
//            getLatest().setStockFeedItem(mines.poll());
//            while (!mines.isEmpty())
//            {
//                addCartLine(mines.poll());
////                getLatest().setStockFeedItem(pendingStoreFeeds.poll());
//            }
//            pendingStoreFeeds=new ConcurrentLinkedQueue<>();
////            return;
//        }
//
//        if(pendingStoreFeeds==null)
//            return;




        List<StockFeedItem> items=new ArrayList<>();

        Boolean hasEmpty=false;
        for(CartLine line:cartLines) {
            if (line.getStockFeedItem() == null) {
                hasEmpty = true;

            }
        }

        int num=cartLines.size();
        if(num>1)
        {
            if(cartLines.get(num-1).getStockFeedItem()!=null&&cartLines.get(num-2).getStockFeedItem()!=null)
            {
                if(Objects.equals(cartLines.get(num - 1).getStockFeedItem().getItem().getUid(), cartLines.get(num - 2).getStockFeedItem().getItem().getUid()))
                {
                    Double numQuantity=MyUtils.getDouble(cartLines.get(num - 2).getCartLineFeedDataReceiver().getQuantity().getText());
                    Double inQuantity=MyUtils.getDouble(cartLines.get(num - 1).getCartLineFeedDataReceiver().getQuantity().getText());
                    cartLines.get(num - 2).getCartLineFeedDataReceiver().getQuantity().setText(String.valueOf(numQuantity+inQuantity));
                    cartLines.get(num - 1).setStockFeedItem(null);
                    hasEmpty=true;
                }
            }

        }

        if(!hasEmpty)
        {
//            System.out.println("adding "+cartLines);
            addCartLine(null);
        }

        if(!pendingStoreFeeds.isEmpty()) {
            getLatest().setStockFeedItem(pendingStoreFeeds.poll());
//            return;
        }

    }

    private Queue<StockFeedItem> pendingStoreFeeds=new ConcurrentLinkedDeque<StockFeedItem>();
    private void addCartLine(StockFeedItem stockFeedItem) {
        CartLine toAdd=new CartLine(stockFeedItem,dbsession,loggedUser);
        listPanel.add(toAdd);
        cartLines.add(toAdd);
        listPanel.repaint();
        toAdd.setSuccessStockFeed(()->{
            new Thread(this::ensureEmptyCartLine).start();
        });
        toAdd.grabFocus();
        listPanel.invalidate();
        listPanel.revalidate();
        listPanel.repaint();
        toAdd.gridItemSelectedRunnable=gridItemSelectedRunnable;
        toAdd.grabFocus();
    }

    private GridItemSelectedRunnable gridItemSelectedRunnable=new GridItemSelectedRunnable() {
        @Override
        public void run() {
//            List<GridItemJpanel> goodItemJpanels=new ArrayList<>();
            for(GridItemJpanel i:getGridItemJpanels())
            {
                Double quantity=MyUtils.getDouble(i.getQuantity().getText());
                if(quantity>0&&i.getItem()!=null)
                {
//                    goodItemJpanels.add(i);
                    StockFeedItem stockFeedItem=new StockFeedItem();
                    stockFeedItem.setItem(i.getItem());
                    stockFeedItem.setQuantity(quantity);
                    stockFeedItem.setBuyingPrice(i.getItem().getBuyingPrice());
                    stockFeedItem.setSellingPrice(i.getItem().getSellingPrice());
//
//                    getLatest().setStockFeedItem(stockFeedItem);
//                    addCartLine(null);
                    pendingStoreFeeds.offer(stockFeedItem);

                }
            }
            ensureEmptyCartLine();

        }
    };

    private List<CartLine> cartLines=new LinkedList<>();

    public CartLine getLatest()
    {
        CartLine tmp=cartLines.get(cartLines.size()-1);
        if(tmp.getStockFeedItem()==null)
            return tmp;
        else
        {
            addCartLine(null);
            return  getLatest();
        }
    }

}
