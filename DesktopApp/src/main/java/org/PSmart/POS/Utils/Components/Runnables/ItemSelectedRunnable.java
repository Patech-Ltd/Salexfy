package org.PSmart.POS.Utils.Components.Runnables;

import org.PSmart.POS.Entities.Item;

public abstract class ItemSelectedRunnable implements Runnable {

    private Item clickedItem;

    public void setClickedItem(Item clickedItem) {
        this.clickedItem = clickedItem;
    }

    public Item getClickedItem() {
        return clickedItem;
    }
}
