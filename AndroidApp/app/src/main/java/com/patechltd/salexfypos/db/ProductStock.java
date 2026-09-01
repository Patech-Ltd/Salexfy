package com.patechltd.salexfypos.db;

import com.patechltd.salexfypos.db.entity.Product;

public class ProductStock {

    public Product product;

    public double currentQty;

    public double stockValue;

    public String categoryName;

    public String brandName;

    /** Resolved display label of the base (retail) unit. */
    public String unitLabel;
}
