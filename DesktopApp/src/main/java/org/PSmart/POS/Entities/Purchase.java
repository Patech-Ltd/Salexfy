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
@Table(name = "PurchaseTable")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private Long createdAt;

    @ManyToOne
    private User createdBy;

    private String supplierName;

    private String invoiceNumber;

    private String notes;

    private Double subtotal;

    private Double taxAmount;

    private Double total;

    /** Amount paid to the supplier at purchase time. null = fully paid (legacy rows). */
    private Double paidAmount;

    private String status;

    private boolean deleted;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PurchaseItem> items;

    public String getReferenceNumber() {
        return "PUR-" + String.format("%06d", uid.hashCode() & 0xFFFFFF);
    }
}
