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
@Table(name = "ExpenseTable")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private Long createdAt;

    @ManyToOne
    private User createdBy;

    private String category;

    private String description;

    private Double amount;

    private String paymentMethod;

    private String receiptNumber;

    private String notes;

    private boolean deleted;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ExpenseItem> items;

    public String getReferenceNumber() {
        return "EXP-" + String.format("%06d", uid.hashCode() & 0xFFFFFF);
    }
}
