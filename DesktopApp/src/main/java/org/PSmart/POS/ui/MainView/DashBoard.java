package org.PSmart.POS.ui.MainView;

import org.PSmart.POS.Entities.User;
import org.PSmart.POS.ui.MainView.ui.admin.AdminPanel;
import org.PSmart.POS.ui.MainView.ui.cashier.CashierPanel;
import org.PSmart.POS.ui.MainView.ui.customers.CustomersPanel;
import org.PSmart.POS.ui.MainView.ui.notifications.NotificationsPanel;
import org.PSmart.POS.ui.MainView.ui.sales.SalesPanel;
import org.PSmart.POS.ui.MainView.ui.stock.StockPanel;
import org.PSmart.POS.Utils.IconsUtils;
import org.PSmart.POS.Utils.MyUtils;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;


public class DashBoard extends JFrame {

    public Runnable logoutAction;

    private Session dbsession;
    private User loggedUser;

    private JPanel header;

    private JPanel footer;

    private JPanel playground;

    private Dimension len=Toolkit.getDefaultToolkit().getScreenSize();
    public DashBoard(Session dbsession, User loggedUser) {
        super();
        this.dbsession=dbsession;
        this.loggedUser=loggedUser;
        setTitle("PSMART POS");


    }



    public void draw()
    {
        setLocation(0,0);
        setSize(len.width,len.height-50);

        setVisible(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        addHeader();

        addFooter();

        playground=new JPanel();
        playground.setLayout(null);
        playground.setBackground(Color.WHITE);

        playground.setPreferredSize(new Dimension(getWidth(),getHeight()-header.getHeight()-footer.getPreferredSize().height));

        add(playground,BorderLayout.CENTER);
        repaint();

    }

    private void addFooter() {
        footer=new JPanel();
        footer.setBackground(Color.WHITE);
        footer.setLayout(new GridLayout(1,1));
        footer.setSize(getWidth(),100);

        footer.setPreferredSize(new Dimension(len.width, 50));


        JPanel ftcontainer=new JPanel();
        ftcontainer.setLayout(null);
        ftcontainer.setSize(getWidth(),footer.getPreferredSize().height);
        ftcontainer.setLocation(0,0);
//        ftcontainer.setBounds(0,0,len.width,80);

        ftcontainer.setBackground(Color.CYAN);

//        footer.add(new JButton("yees"));
        footer.add(ftcontainer);





        footer.repaint();


        addItems(ftcontainer);

        getContentPane().add(footer,BorderLayout.SOUTH);





    }



    private void addHeader() {
        header=new JPanel();
        header.setBackground(Color.WHITE);
        header.setLayout(new GridLayout(1,15));
        header.setSize(getWidth(),100);
//        header.setPreferredSize(new Dimension(getWidth(),80));
        header.setMaximumSize(new Dimension(getWidth(),80));



        addMenus(header);

        getContentPane().add(header,BorderLayout.NORTH);

    }

    private void addItems(JPanel footer) {



        int footerHeight=footer.getPreferredSize().height;

        JLabel adv=new JLabel("<html><center>PSMART POS<br>+254 723 466 505</html>",JLabel.CENTER);
        adv.setFont(MyUtils.courierFont(15));
        adv.setForeground(new Color(0,0,139));

        int location=0;

        adv.setLocation(location,0);
        adv.setSize(getWidth()/6,footerHeight);

        footer.add(adv);

        location+=getWidth()/6;

        MyUtils.drawVerticalLine(footer,location);



        JLabel ll=new JLabel("Active User",JLabel.CENTER);
        ll.setSize(getWidth()/12,footerHeight);
        ll.setLocation(location,0);
        footer.add(ll);

        location+=getWidth()/12;
        MyUtils.drawVerticalLine(footer,location);

        JLabel cUser=new JLabel(loggedUser.getFullname(),JLabel.CENTER);
        cUser.setForeground(new Color(0,0,139));
        cUser.setFont(MyUtils.courierFont(23));
        cUser.setSize(getWidth()/3,footerHeight);
        cUser.setLocation(location,0);

        location+=getWidth()/3;
        MyUtils.drawVerticalLine(footer,location);


        footer.add(cUser);

        final JLabel cDate=new JLabel("<html><center>22-Feb-2000<br>11:22:01</html>",JLabel.CENTER);
        cDate.setFont(MyUtils.courierFont(18));
        cDate.setSize(getWidth()/6,footerHeight);
        cDate.setLocation(location,0);

        location+=getWidth()/6;
        MyUtils.drawVerticalLine(footer,location);

        footer.add(cDate);



        JButton logout=new JButton();
        logout.setBackground(new Color(255, 0, 0));
        logout.setText("<html><center>Log Out</html>");
        logout.setForeground(Color.WHITE);
        logout.setFont(MyUtils.arialFont(20));
        logout.setSize(200,footer.getPreferredSize().height);
        logout.setLocation(footer.getWidth()-200,0);
        logout.setToolTipText("Sign out from current user");
        try {
            int height=35;
            BufferedImage img=IconsUtils.getIcon("logout.png");
            final Image image = img.getScaledInstance(img.getWidth()*height/img.getHeight(),height, Image.SCALE_SMOOTH);
            if(image!=null)
                logout.setIcon(new ImageIcon(image));
        }catch (Exception es)
        {
            es.printStackTrace();
        }
        logout.addActionListener((e)->{
            int ans=JOptionPane.showConfirmDialog(null,"You Sure you want to Log out ?","Confirm Logout", JOptionPane.YES_NO_OPTION);
            if(ans==JOptionPane.YES_OPTION)
            {
                System.out.println("noew Logout");
                if(logoutAction!=null)
                    logoutAction.run();
            }
        });

        footer.add(logout);



        new Thread(()->{
            while (isVisible())
            {
                cDate.setText(MyUtils.getCurrentDateFormatted());
//                cDate.repaint();
//                System.out.println("updating date ");

                try{
                    Thread.sleep(1000);

                }catch (Exception es)
                {
                    es.printStackTrace();
                }
            }
        }).start();

    }

    private JButton[] menuItems;
    private Runnable[] tabRunnables;
    private void addMenus(JPanel header) {

        //add Button s and icons
        String[][] buttons=getButtons();

        final Runnable[] runnables=getRunnables();
        tabRunnables = runnables;

        menuItems=new JButton[buttons.length];

        for(int i=0;i<menuItems.length;i++)
        {
            String[] btn=buttons[i];
            menuItems[i] = MyUtils.createButton("<html><center><body style='color:white;padding:15px;'>"+btn[0]+"</body></html>",new Color(25, 118, 211));//new JButton(new ImageIcon(IconsUtils.getIcon(btn[1])));
            menuItems[i].setFont(new Font("Courier",Font.BOLD,15));
            menuItems[i].setForeground(Color.WHITE);
            menuItems[i].setToolTipText(btn[0].replaceAll("<br>"," "));

            try {
                int height=40;
               BufferedImage img=IconsUtils.getIcon(btn[1]);

                final Image image = img.getScaledInstance(img.getWidth()*height/img.getHeight(),height, Image.SCALE_SMOOTH);
               if(image!=null)
                menuItems[i].setIcon(new ImageIcon(image));

           }catch (Exception es)
           {
               es.printStackTrace();
           }
            int finalI = i;
            menuItems[i].addActionListener((e)->{
//                System.out.println("clicked and set");
//                menuItems[finalI].setBackground(new Color(0,0,139));
                setActiveTab(finalI);

                runnables[finalI].run();
            });
            header.add(menuItems[i]);


        }

    }

    private HashMap<String,JPanel> fragments=new HashMap<>();

    private Runnable[] getRunnables() {
        final Runnable[] runnables = new Runnable[]{
                (() -> showFragment("cashier",
                        new CashierPanel(loggedUser, this))),
                (() -> showFragment("stock",
                        new StockPanel(this))),
                (() -> showFragment("sales",
                        new SalesPanel())),
                (() -> showFragment("customers",
                        new CustomersPanel())),
                (() -> showFragment("purchases",
                        new org.PSmart.POS.ui.MainView.ui.purchases.PurchasesPanel())),
                (() -> showFragment("admin",
                        new AdminPanel(new StockPanel(this)))),
                (() -> showFragment("settings",
                        new org.PSmart.POS.ui.MainView.ui.settings.SettingsPanel())),
                (() -> showFragment("notifications",
                        new NotificationsPanel())),
        };
        return runnables;
    }

    private void showFragment(String key, JPanel builder) {
        playground.removeAll();
        if (!fragments.containsKey(key))
            fragments.put(key, builder);
        JPanel frag = fragments.get(key);
        frag.removeAll();
        frag.setLayout(null);
        frag.setBounds(0, 0, playground.getWidth(), playground.getHeight());
        frag.setPreferredSize(new Dimension(playground.getWidth(), playground.getHeight()));
        frag.setVisible(true);
        playground.add(frag, 0);
        playground.revalidate();
        playground.repaint();
        frag.revalidate();
        frag.repaint();
    }

    private String[][] getButtons() {
        return new String[][]{
                {"Cashier<br>Checkout","cart.png","checkout"},
                {"Stock/Inventory<br>Management","stock.png","stock"},
                {"Sales<br>Reports","reports.png","sales"},
                {"Customers","debtors.png","debtors"},
                {"Purchases &<br>Expenses","stock.png","purchases"},
                {"Admin","admin.png","admin"},
                {"Settings","img.png","settings"},
                {"Notifications","bell.png","notification"}

        };
    }

    public void setActiveTab(int tab)
    {
        if (menuItems == null) return;
        for(int i=0;i<menuItems.length;i++)
        {
            if(menuItems[i]==null) continue;
            if(i!=tab)
                menuItems[i].setBackground(new Color(25, 118, 211));
            else
                menuItems[i].setBackground(new Color(0,0,139));

        }

    }

    /** Programmatically open a tab by index (used by other panels, e.g. cashier -> sales). */
    public void openTab(int tab) {
        setActiveTab(tab);
        if (tabRunnables != null && tab >= 0 && tab < tabRunnables.length && tabRunnables[tab] != null) {
            tabRunnables[tab].run();
        }
    }

    public javax.swing.JPanel getPlayground() {
        return playground;
    }


    public void setTab(int tab)
    {
        getRunnables()[tab].run();
    }

    public Session getDbsession() {
        return dbsession;
    }

    public User getLoggedUser() {
        return loggedUser;
    }
}
