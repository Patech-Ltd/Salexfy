package org.PSmart.POS.Utils;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class DecimalDocumentFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        System.out.println("checking "+fb.getDocument().getLength());
        if (string.matches("\\d*\\.?\\d*")&&isValid(fb.getDocument().getText(0,fb.getDocument().getLength())+string)) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text.matches("\\d*\\.?\\d*")&&isValid(fb.getDocument().getText(0,fb.getDocument().getLength())+text)) {
            super.replace(fb, offset, length, text, attrs);
        }
    }

    public Boolean isValid(String str)
    {
        System.out.println("checking "+str);
        try{
            Double.parseDouble("4"+str+"4");
            return true;
        }catch (Exception es)
        {
            es.printStackTrace();
            return false;
        }
    }
}