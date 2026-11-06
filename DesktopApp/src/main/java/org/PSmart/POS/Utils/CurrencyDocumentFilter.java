package org.PSmart.POS.Utils;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.text.NumberFormat;

public class CurrencyDocumentFilter extends DocumentFilter {
        private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            super.insertString(fb, offset, text, attr);
            validateCurrency(fb.getDocument().getText(0, fb.getDocument().getLength()));
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            super.replace(fb, offset, length, text, attrs);
            validateCurrency(fb.getDocument().getText(0, fb.getDocument().getLength()));
        }

        private void validateCurrency(String text) {
            try {
                // Try parsing the text as a currency
                currencyFormat.parse(text);
            } catch (Exception e) {
                //Toolkit.getDefaultToolkit().beep();
            }
        }

}