package org.PSmart.POS.Utils;

import org.PSmart.POS.Entities.Item;

public abstract class EditItemRunnable implements Runnable {

    private Item itemCategory;

    public Item getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(Item itemCategory) {
        this.itemCategory = itemCategory;
    }
}
