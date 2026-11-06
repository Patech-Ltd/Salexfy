package org.PSmart.POS.ui.Login;

import org.PSmart.POS.Entities.User;
import org.hibernate.Session;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class LoginWindowOld {

    private Session dbsession;
    public ActionListener action;

    private int attempts;

    public LoginWindowOld(Session session, ActionListener action)
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
        JPanel form=new JPanel();
        login.add(form);


        form.setVisible(true);
        form.setLocation(home.getWidth()/4,(home.getHeight()/10));
        float wit=home.getWidth()/2;
        float hit=home.getHeight()/2;
        form.setSize(home.getWidth()/2,home.getHeight()-home.getHeight()/4);
        form.setBackground(Color.white);
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
            panel.setBounds(home.getWidth()/4-nh/2, 20, nh, boundHeight);  // set the location and size of the panel
            panel.setOpaque(false);  // make the panel transparent
            form.add(panel);
        } catch (Exception e) {
            e.printStackTrace();
        }






        JLabel head=new JLabel("login",JLabel.CENTER);
        head.setText("LOGIN");
        head.setLocation(0,home.getHeight()/5+50);
        head.setVisible(true);
        head.setSize(home.getWidth()/2,35);
        form.add(head);
        head.setLayout(null);
        head.setFont(new Font("Arial",Font.BOLD,20));
        head.setForeground(new Color(0, 0, 144));


        JLabel name=new JLabel("name");
        name.setText("USER_NAME");
        name.setLocation((home.getWidth()/8),(home.getHeight()/2)/4+home.getHeight()/5);
        name.setVisible(true);
        form.add(name);
        name.setLayout(null);
        name.setFont(new Font("Arial",Font.BOLD,18));
        name.setForeground(Color.blue);
        name.setSize(149,40);

        JLabel password=new JLabel();
        password.setText("PASSWORD");
        password.setLocation((home.getWidth()/8),(home.getHeight()/2)/2+home.getHeight()/5);
        password.setVisible(true);
        form.add(password);
        password.setLayout(null);
        password.setFont(new Font("Arial",Font.BOLD,18));
        password.setForeground(Color.blue);
        password.setSize(149,40);


        JTextField usrname=new JTextField();
        usrname.setLocation((home.getWidth()/4),(home.getHeight()/2)/4+10+home.getHeight()/5);
        usrname.setVisible(true);
        form.add(usrname);
        usrname.setLayout(null);
        usrname.setFont(new Font("Arial",Font.BOLD,13));
        usrname.setForeground(Color.black);
        usrname.setSize(199,20);
        usrname.setText("");



        JPasswordField passwd=new JPasswordField();
        passwd.setLocation((home.getWidth()/4),(home.getHeight()/2)/2+10+home.getHeight()/5);
        passwd.setVisible(true);
        form.add(passwd);
        passwd.setText("");
        passwd.setLayout(null);
        passwd.setFont(new Font("Arial",Font.BOLD,13));
        passwd.setForeground(Color.black);
        passwd.setSize(199,20);

        JButton logna=new JButton("LOGIN");
        logna.setVisible(true);
        form.add(logna);
        logna.setBackground(Color.blue);
        logna.setFont(new Font("Arial",Font.BOLD,20));
        logna.setForeground(Color.white);
        logna.setSize(149,40);
        logna.setLocation((home.getWidth()/4-74),(home.getHeight()-home.getHeight()/4-50)-home.getHeight()/10);
        logna.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String username=usrname.getText();
                String password=passwd.getText();

                try{
                    // Create an HQL query to fetch a user by username
                    String hql = "FROM User u WHERE u.username = :username";
                    User user = dbsession.createQuery(hql, User.class)
                            .setParameter("username",username)
                            .uniqueResult();

                    if (user == null) {
                        System.out.println("User not found.");

                        attempts--;
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(null, "User Not Found \n "+attempts+" attempts Remaining", "Login Unsuccessful", JOptionPane.ERROR_MESSAGE);

                    } else {
                        System.out.println("User found: " + user.getUsername());

                        if(user.getPassword().equals(password))
                        {
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




        login.setBackground(Color.decode("#0047ab")); // Deep Blue

        form.setBackground(Color.white);

        head.setForeground(Color.decode("#3f3f3f")); // Rich Grey

        name.setForeground(Color.decode("#0047ab")); // Deep Blue
        password.setForeground(Color.decode("#0047ab")); // Deep Blue

        usrname.setForeground(Color.decode("#3f3f3f")); // Rich Grey
        passwd.setForeground(Color.decode("#3f3f3f")); // Rich Grey

        logna.setBackground(Color.decode("#3f3f3f")); // Rich Grey
        logna.setForeground(Color.white);

    }
}
