package org.PSmart.POS.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * A single entry recorded on a customer's credit account. Every on-credit sale
 * posts a CHARGE and every payment against the debt posts a PAYMENT. The running
 * balance is stored on the CreditTransaction and mirrored on Customer.currentBalance.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CreditTransactionTable")
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private Customer customer;

    @ManyToOne
    private Sale sale;

    /** CHARGE or PAYMENT. */
    private String type;

    /** Amount of this transaction (positive for charges and payments). */
    private Double amount;

    /** Running balance after this transaction is applied. */
    private Double balanceAfter;

    @ManyToOne
    private User cashier;

    private Long createdAt;

    private String notes;

    private boolean deleted;
}