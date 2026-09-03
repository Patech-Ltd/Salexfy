package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "purchase_items",
        foreignKeys = @ForeignKey(entity = Purchase.class,
                parentColumns = "id", childColumns = "purchaseId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("purchaseId"), @Index("productId")})
public class PurchaseItem {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String purchaseId;

    public String productId;

    public String productName;

    public String barcode;

    public double qty;

    public double stockQty;

    public String unitLabel;

    /** Base units contained in 1 of this purchase unit. */
    public double factor = 1;

    public boolean isWholesale;

    public double unitPrice;

    public double lineTotal;
}
