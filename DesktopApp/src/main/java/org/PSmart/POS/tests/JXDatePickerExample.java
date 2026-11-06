package org.PSmart.POS.tests;

import org.jdesktop.swingx.JXDatePicker;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JXDatePickerExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("JXDatePicker Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel panel = new JPanel();

            JXDatePicker datePicker = new JXDatePicker();
            JButton showDateButton = new JButton("Show Selected Date");

            showDateButton.addActionListener(e -> {
                Date selectedDate = datePicker.getDate();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                JOptionPane.showMessageDialog(frame, "Selected Date: " + dateFormat.format(selectedDate));
            });

            panel.add(datePicker);
            panel.add(showDateButton);

            frame.getContentPane().add(panel);
            frame.setSize(400, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
