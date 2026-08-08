package com.patechltd.salexfypos.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;

import java.util.List;

public class PurchaseWithItems {

    @Embedded
    public Purchase purchase;

    @Relation(parentColumn = "id", entityColumn = "purchaseId")
    public List<PurchaseItem> items;
}
