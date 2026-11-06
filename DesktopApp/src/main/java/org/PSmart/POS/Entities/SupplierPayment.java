package org.PSmart.POS.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Money paid to a supplier against what the shop owes. */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "SupplierPaymentTable")
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private String supplierName;
    private Double amount;
    private String method;
    private String notes;
    private String createdByName;
    private Long createdAt;
    private boolean deleted;
}
