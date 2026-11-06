package org.PSmart.POS.tests;

import org.jdatepicker.JDatePicker;
import org.jdatepicker.UtilDateModel;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

public class DateSelectorExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Date Selector Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel panel = new JPanel();

            UtilDateModel model = new UtilDateModel();
            Properties properties = new Properties();
            properties.put("text.today", "Today");
            properties.put("text.month", "Month");
            properties.put("text.year", "Year");

            model.setValue(new Date());

            JDatePicker datePicker = new JDatePicker(model,"E, dd-MMM-YYYY");

            JButton showDateButton = new JButton("Show Selected Date");

            showDateButton.addActionListener(e -> {
                Date selectedDate = (Date) datePicker.getModel().getValue();
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
