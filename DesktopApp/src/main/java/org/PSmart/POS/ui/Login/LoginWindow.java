package org.PSmart.POS.ui.Login;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.*;
import org.PSmart.POS.Utils.Components.PPasswordTextField;
import org.PSmart.POS.Utils.Components.PTextField;
import org.hibernate.Session;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;

public class LoginWindow {

    private Session dbsession;
    public ActionListener action;

    private int attempts;

    private User successUser;

    public LoginWindow(Session session,ActionListener action)
    {
        dbsession=session;
        this.action=action;
        this.attempts=3;

        displayLogin();

    }

    public JFrame home;

    private void displayLogin() {
        home=new JFrame("Login Window");
        Dimension len=Toolkit.getDefaultToolkit().getScreenSize();
        home.setLocation(0,0);

        //home.setSize(home.getWidth(),home.getHeight());
       // home.setLocation(len.width/4,len.height/4);

        home.setSize(len.width,len.height);

        home.setExtendedState(JFrame.MAXIMIZED_BOTH);

        home.setVisible(true);
        //home.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


//start of login window
        JPanel login=new JPanel();
        home.add(login);
        login.setVisible(true);
        login.setLocation(0,0);
        login.setSize(home.getWidth(),home.getHeight());
        login.setBackground(Color.blue);
        login.setLayout(null);
        login.setBackground(new Color(191,191,191));

        JPanel form=new JPanel();
        login.add(form);
        form.setVisible(true);
        form.setLocation(home.getWidth()/6,home.getHeight()/2-250);
        form.setSize(home.getWidth()*2/3,500);
        form.setBackground(new Color(25, 118, 211));
        form.setLayout(null);


        try {

            InputStream logoStream = LoginWindowOld.class.getResourceAsStream("/logo.png");

            final BufferedImage originalImage = ImageIO.read(logoStream);//new File("clistecherry.jpg"));

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int boundHeight = home.getHeight()/5;  // or 200 if you prefer

            int newHeight = originalHeight;
            int newWidth = originalWidth;

            // If original image height is bigger than bound
            if (originalHeight > boundHeight) {
                //scale height to fit
                newHeight = boundHeight;
                //scale width to maintain aspect ratio
                newWidth = (newHeight * originalWidth) / originalHeight;
            }

            final Image image = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

            // Create new final variables for width and height for the inner class
            final int finalNewWidth = newWidth;
            final int finalNewHeight = newHeight;

            // Create a new custom JPanel
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    // calculate the coordinates to draw the image at the center of the panel
                    int x = (getWidth() - finalNewWidth) / 2;
                    int y = (getHeight() - finalNewHeight) / 2;
                    g.drawImage(image, x, y, this);  // draw image at the center of this panel
                }
            };

            int nh=finalNewWidth;//x;//boundHeight*(originalWidth/originalHeight)+50;
            panel.setBounds(0,0,form.getWidth()/2,form.getHeight());  // set the location and size of the panel
            panel.setOpaque(true);  // make the panel transparent
            panel.setBackground(Color.WHITE);
            form.add(panel);
            form.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }


        JLabel head=new JLabel("login",JLabel.CENTER);
        head.setText("Login");
        head.setLocation(form.getWidth()/2,30);
        head.setVisible(true);
        head.setSize(form.getWidth()/2,35);
        form.add(head);
        head.setLayout(null);
        head.setFont(new Font("Courier",Font.BOLD,35));
        head.setForeground(new Color(0, 0, 144));
        head.setForeground(Color.WHITE);

        JLabel ll=new JLabel("Hi! Lets get started.",JLabel.CENTER);
        ll.setBackground(Color.WHITE);
        ll.setLocation(form.getWidth()/2,90);
        ll.setSize(form.getWidth()/2,20);
        ll.setForeground(Color.WHITE);

        form.add(ll);



        PTextField pTextField=new PTextField(new Color(200,200,200),Color.WHITE,"Username", IconsUtils.getIcon("person.png"));
        pTextField.setLocation(form.getWidth()/2+form.getWidth()/12,150);
        pTextField.setSize(form.getWidth()/2-form.getWidth()/6,70);

        form.add(pTextField);


        PPasswordTextField pPasswordTextField=new PPasswordTextField(new Color(200,200,200),Color.WHITE,"Password");
        pPasswordTextField.setLocation(form.getWidth()/2+form.getWidth()/12,250);
        pPasswordTextField.setSize(form.getWidth()/2-form.getWidth()/6,70);

        form.add(pPasswordTextField);



        JButton logna=MyUtils.createRoundedButton("LOGIN",new Color(255,255,255));
        logna.setVisible(true);
        form.add(logna);

        form.repaint();

        pTextField.getField().grabFocus();
        pTextField.setAction(()->{
            pPasswordTextField.grabFocus();
        });

        pPasswordTextField.setAction(()->{
            logna.grabFocus();
            logna.doClick();
        });

        logna.setFont(new Font("Courier",Font.BOLD,20));
        logna.setForeground(new Color(25, 118, 211));
        logna.setSize(form.getWidth()/2-form.getWidth()/6,40);
        logna.setBackground(Color.WHITE);
        logna.setLocation(form.getWidth()/2+form.getWidth()/12,400);
        logna.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String username=pTextField.getField().getText().trim().toLowerCase();
                String password= pPasswordTextField.getText().trim();

                try{
                    // Create an HQL query to fetch a user by username
                    String hql = "FROM User u WHERE u.username = :username";
                    User user =  DatabaseUtils.getInstance().getEmf().createEntityManager().createQuery(hql, User.class)
                            .setParameter("username",username)
                            .getSingleResult();

                    if (user == null) {
                        System.out.println("User not found.");
                        try{
                            List users = DatabaseUtils.getInstance().getEmf().createEntityManager().createQuery("FROM User u")
                                    .getResultList();
                            System.out.println(users);
                        }catch (Exception es){
                            es.printStackTrace();
                        }

                        attempts--;
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(null, "User Not Found \n "+attempts+" attempts Remaining", "Login Unsuccessful", JOptionPane.ERROR_MESSAGE);

                    } else {
                        System.out.println("User found: " + user.getUsername());

                        if(user.getPassword().equals(password))
                        {
                            successUser=user;
                            action.actionPerformed(e);
                        }
                        else
                        {
                            attempts--;
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(null, "Incorrect Password \n "+attempts+" attempts Remaining", "Login Unsuccessful", JOptionPane.ERROR_MESSAGE);

                        }
                    }

                }catch (Exception ed)
                {
                    ed.printStackTrace();
                }
                if(attempts<=0)
                {
                    JOptionPane.showMessageDialog(null, "You have 0 attempts left. Try again Later", "Incorrect Login 3 Times", JOptionPane.ERROR_MESSAGE);
                    home.dispose();
                }

            }});



    }

    private boolean tryit(JPanel form) {

        PTextField pTextField=new PTextField(new Color(200,200,200),Color.WHITE,"Username", IconsUtils.getIcon("person.png"));
        pTextField.setLocation(form.getWidth()/2+50,50);
        pTextField.setSize(form.getWidth()/2-100,70);

        form.add(pTextField);


        PPasswordTextField pPasswordTextField=new PPasswordTextField(new Color(200,200,200),Color.WHITE,"Password");
        pPasswordTextField.setLocation(form.getWidth()/2+50,150);
        pPasswordTextField.setSize(form.getWidth()/2-100,70);

        form.add(pPasswordTextField);

        form.repaint();

        return true;
    }


    public User successfulUserLogin()
    {
        return successUser;
    }
}
