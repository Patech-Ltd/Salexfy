package org.PSmart.POS.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "SaleTable")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private Long createdAt;

    @ManyToOne
    private User cashier;

    @ManyToOne
    private Customer customer;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<SaleItem> items;

    private Double subtotal;

    private Double discount;

    private Double vatAmount;

    private Double total;

    private Double amountPaid;

    private Double change;

    private String paymentMethod;

    private String status;

    private boolean deleted;

    /** Set when the sale is cancelled (status becomes VOIDED); the sale stays in the books for audit. */
    private Long voidedAt;
    private String voidReason;
    private String voidedByName;

    /** Loyalty movements booked by this sale, so a void can reverse them exactly. */
    private Double pointsEarned;
    private Double pointsRedeemed;

    public String getReceiptNumber() {
        return "RCP-" + String.format("%06d", uid.hashCode() & 0xFFFFFF);
    }
}
