package org.PSmart.POS.tests;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AutoClosingNotification extends JDialog {

    public AutoClosingNotification(JFrame parent, String message, int timeout) {
        super(parent, true);
        setUndecorated(true);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(300, 100));
        Dimension len=Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(len.width/2-150,len.height-200);

        setSize(300,50);


        JLabel label = new JLabel(message, SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);

        Timer timer = new Timer(timeout, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Close the notification after the specified timeout
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Auto Closing Notification Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 300);

            JButton showNotificationButton = new JButton("Show Notification");
            showNotificationButton.addActionListener(e -> {
                AutoClosingNotification notification = new AutoClosingNotification(frame, "This is a notification", 2000);
                notification.setVisible(true);
            });

            frame.getContentPane().add(showNotificationButton, BorderLayout.CENTER);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
        });
    }


    public static void sendNotification(String Message)
    {
        new Thread(()->{
            AutoClosingNotification notification = new AutoClosingNotification(null, Message, 2000);
            notification.setVisible(true);
        }).start();
    }
}
