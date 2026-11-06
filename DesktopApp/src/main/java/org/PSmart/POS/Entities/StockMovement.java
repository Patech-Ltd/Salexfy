package org.PSmart.POS.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Append-only stock ledger entry for manual changes: adjustments, damages, stock-take
 * variances. On-hand = received (StockFeedItem) - sold (SaleItem) + SUM(StockMovement.quantityDelta).
 * Additive table only; nothing existing is altered.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "StockMovementTable")
public class StockMovement {

    public static final String ADJUSTMENT = "ADJUSTMENT";
    public static final String STOCK_TAKE = "STOCK_TAKE";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private Item item;

    /** ADJUSTMENT or STOCK_TAKE. */
    private String type;

    /** Signed change applied to on-hand (negative = removed). */
    private Double quantityDelta;

    /** On-hand before / after, captured at the time for audit. */
    private Double quantityBefore;
    private Double quantityAfter;

    private String reason;
    private String notes;

    /** Groups all lines of one stock-take session. */
    private String reference;

    private String createdByName;
    private Long createdAt;

    private boolean deleted;
}
