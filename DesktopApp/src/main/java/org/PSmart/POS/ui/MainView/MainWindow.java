package org.PSmart.POS.ui.MainView;

import org.PSmart.POS.Entities.User;
import org.hibernate.Session;

import javax.swing.*;

public class MainWindow {
    private Session dbsession;
    private User loggedUser;

    private DashBoard dashBoard;


    public MainWindow()
    {


    }


    public MainWindow(Session session, User loggedUser) {
        this.dbsession=session;
        this.loggedUser=loggedUser;

        show();
    }

    public void show()
    {
        dashBoard= new DashBoard(dbsession,loggedUser);

        dashBoard.draw();
        dashBoard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);



    }

    public void setLogoutAction(Runnable runnable)
    {
        dashBoard.logoutAction=runnable;
    }

    public void dispose(){
        if(dashBoard!=null)
            dashBoard.dispose();

        loggedUser=null;
    }

    public void setTab(int tab)
    {
        dashBoard.setTab(tab);
    }

    public void setSize(int i, int i1) {
        dashBoard.setSize(i,i1);
    }

    public void repaint() {
        dashBoard.repaint();
    }
}
