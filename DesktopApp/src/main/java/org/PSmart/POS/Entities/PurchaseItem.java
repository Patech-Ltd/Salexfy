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
@Table(name = "PurchaseItemTable")
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private Purchase purchase;

    @ManyToOne
    private Item item;

    private Double quantity;

    private Double unitPrice;

    private Double total;

    private boolean deleted;
}
