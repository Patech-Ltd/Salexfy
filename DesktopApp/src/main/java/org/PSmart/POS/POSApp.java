package org.PSmart.POS;


import org.PSmart.POS.Entities.User;
import org.PSmart.POS.ui.Login.LoginWindow;

import org.PSmart.POS.ui.MainView.MainWindow;
import org.hibernate.Session;

public class POSApp {

    private Session dbsession;
    private User loggedUser;

    private MainWindow mainWindow;

    private LoginWindow loginWindow;

    public static String imagesSavePath="images/";




    POSApp(Session session)
    {
        this.dbsession=session;
        this.loggedUser=null;



//        new MainMenu(session,loggedUser);

        loginWindow =new LoginWindow(dbsession,null);




        Runnable logoutAction=new Runnable() {
            @Override
            public void run() {

                loggedUser=null;
                loginWindow=null;
                if(mainWindow!=null)
                    mainWindow.dispose();

                mainWindow=null;

                loginWindow =new LoginWindow(dbsession,null);


                loginWindow.action= e -> {
                    loggedUser=loginWindow.successfulUserLogin();
                    loginWindow.home.dispose();
                    mainWindow=new MainWindow(session,loggedUser);
                    mainWindow.setLogoutAction(this);

                };



            }
        };


        loginWindow.action= e -> {
            this.loggedUser=loginWindow.successfulUserLogin();
            loginWindow.home.dispose();
            mainWindow=new MainWindow(session,loggedUser);
            mainWindow.setLogoutAction(logoutAction);

        };



    }
}
