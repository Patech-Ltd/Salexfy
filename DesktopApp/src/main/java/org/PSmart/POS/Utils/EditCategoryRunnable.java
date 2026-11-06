package org.PSmart.POS.Utils;

import org.PSmart.POS.Entities.ItemCategory;

public abstract class EditCategoryRunnable implements Runnable {

    private ItemCategory itemCategory;

    public ItemCategory getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(ItemCategory itemCategory) {
        this.itemCategory = itemCategory;
    }
}
