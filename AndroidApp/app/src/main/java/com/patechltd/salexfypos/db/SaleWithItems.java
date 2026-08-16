package com.patechltd.salexfypos.db;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;

import java.util.List;

public class SaleWithItems {

    @Embedded
    public Sale sale;

    @Relation(parentColumn = "id", entityColumn = "saleId")
    public List<SaleItem> items;

    @Relation(parentColumn = "id", entityColumn = "saleId")
    public List<SalePayment> payments;
}
