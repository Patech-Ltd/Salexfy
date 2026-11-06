package org.PSmart.POS.Entities;


import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockFeedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private Long ref;

    @ManyToOne
    private Item item;

    private Double buyingPrice;

    private Double sellingPrice;

    private Double quantity;

    private Long expiry;

    private boolean deleted;



}
