package org.PSmart.POS.tests;

import org.jdesktop.swingx.JXSearchField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class SearchableAutoCompleteTextFieldExample extends JFrame {
    private JXSearchField searchField;

    public SearchableAutoCompleteTextFieldExample() {
        super("Searchable AutoComplete JTextField Example");

        List<String> autoCompleteList = new ArrayList<>();
        // Populate the list with your autocomplete suggestions
        autoCompleteList.add("Apple");
        autoCompleteList.add("Banana");
        autoCompleteList.add("Orange");
        autoCompleteList.add("Grapes");

        searchField = new JXSearchField();

        for(String i:autoCompleteList)
            searchField.add(new PopupMenu(i));

        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedValue = (String) searchField.getText();
                JOptionPane.showMessageDialog(SearchableAutoCompleteTextFieldExample.this,
                        "Selected: " + selectedValue);
            }
        });

        // Layout setup
        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        add(searchField);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SearchableAutoCompleteTextFieldExample();
            }
        });
    }
}
