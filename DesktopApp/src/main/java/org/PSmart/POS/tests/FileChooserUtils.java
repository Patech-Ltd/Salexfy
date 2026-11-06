package org.PSmart.POS.tests;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FileChooserUtils {

    public static File selectFile() {
        FileDialog fileDialog = new FileDialog(new Frame(), "Choose an Image File", FileDialog.LOAD);
        fileDialog.setFile("*.jpg;*.jpeg;*.png;*.gif");

        fileDialog.setDirectory(System.getProperty("user.home") + File.separator + "Pictures");
        fileDialog.setVisible(true);
        //fileDialog.setFile(System.getProperty("user.home") + File.separator + "Pictures");

        String selectedFile = fileDialog.getFile();
        String selectedDirectory = fileDialog.getDirectory();

        if (selectedFile != null && selectedDirectory != null) {
            return new File(selectedDirectory, selectedFile);
        } else {
            return null; // User canceled the file selection
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            Frame frame = new Frame("File Chooser Example");
            frame.setSize(300, 200);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    System.exit(0);
                }
            });

            Button chooseFileButton = new Button("Choose Image File");
            chooseFileButton.addActionListener(e -> {
                File selectedFile = selectFile();
                if (selectedFile != null) {
                    JOptionPane.showMessageDialog(frame, "Selected File: " + selectedFile.getAbsolutePath());
                } else {
                    JOptionPane.showMessageDialog(frame, "File selection canceled.");
                }
            });

            frame.add(chooseFileButton, BorderLayout.CENTER);

            frame.setVisible(true);
        });
    }
}
