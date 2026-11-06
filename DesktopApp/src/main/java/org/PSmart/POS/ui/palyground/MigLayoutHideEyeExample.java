package org.PSmart.POS.ui.palyground;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MigLayoutHideEyeExample {
    public static void main(String[] args) {
        JFrame frame = new JFrame("MigLayout Hide Eye Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MigLayout layout = new MigLayout("wrap 2", "[][grow]");
        JPanel panel = new JPanel(layout);

        JLabel passwordLabel = new JLabel("Password:");
        JPasswordField passwordField = new JPasswordField(20);

        JButton toggleButton = new JButton("Show/Hide");
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Toggle password visibility
                passwordField.setEchoChar(passwordField.getEchoChar() == 0 ? '\u2022' : (char) 0);
            }
        });

        panel.add(passwordLabel);
        panel.add(passwordField, "growx");
        panel.add(toggleButton, "span");

        frame.add(panel);
        frame.setSize(300, 150);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
