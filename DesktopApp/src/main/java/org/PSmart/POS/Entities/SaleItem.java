package org.PSmart.POS.Entities;

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
@Table(name = "SaleItemTable")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private Sale sale;

    @ManyToOne
    private Item item;

    private Double quantity;

    /** Selling (unit) price at the time of the sale. */
    private Double unitPrice;

    /** Buying (cost) price captured at the time of the sale — enables real profit/loss. */
    private Double buyingPrice;

    private Double discount;

    private Double total;

    private boolean deleted;
}
