package org.PSmart.POS.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * A persisted line in the cashier's working cart. Persisting the cart means an
 * in-progress sale cannot be lost on app restart, crash or a stray click.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CartLineTable")
public class CartLine {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private Item item;

    private Double quantity;

    private Double unitPrice;

    private Double buyingPrice;

    private Double discount;

    private Long updatedAt;

    private boolean deleted;
}