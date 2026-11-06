package org.PSmart.POS.tests;


import org.PSmart.POS.Entities.User;

import org.PSmart.POS.ui.MainView.ui.stock.stockfeed.popup.FeedStockFrame;

public class Test {
    public static void main(String[] args)
    {
        User sr=new User();
            sr.setFullname("Patrick Githinji Ngunjiri");
            sr.setPhone("0723466505");
            sr.setPassword("123");
            sr.setUsername("pats");

//        MainWindow mainWindow=new MainWindow(org.PSmart.POS.Main.getDbSession(),sr);

//        new FeedStockFrame(nu);

//        mainWindow.setTab(1);
//        mainWindow.repaint();s
//        mainWindow.setSize(200,200);
//        new EditItem(null,org.PSmart.POS.Main.getDbSession(),sr);
        new FeedStockFrame(null,null,sr).setVisible(true);
//        new GridtemsSearch();

        try{
            Thread.sleep(1500);
        }catch (Exception es)
        {

        }
//        mainWindow.setTab(1);


    }
}
