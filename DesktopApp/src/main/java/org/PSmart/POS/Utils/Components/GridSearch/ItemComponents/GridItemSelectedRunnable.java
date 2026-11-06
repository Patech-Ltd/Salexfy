package org.PSmart.POS.Utils.Components.GridSearch.ItemComponents;

import org.PSmart.POS.Entities.Item;

import java.util.List;

abstract public class GridItemSelectedRunnable implements Runnable {

    private List<GridItemJpanel> gridItemJpanels;

    public List<GridItemJpanel> getGridItemJpanels() {
        return gridItemJpanels;
    }

    public void setGridItemJpanels(List<GridItemJpanel> gridItemJpanels) {
        this.gridItemJpanels = gridItemJpanels;
    }
}



