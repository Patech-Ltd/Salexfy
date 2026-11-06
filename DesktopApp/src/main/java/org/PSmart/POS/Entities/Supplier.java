package org.PSmart.POS.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Supplier contact card. Purchases link to it by (case-insensitive) name. */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "SupplierTable")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private String name;
    private String phone;
    private String email;
    private String address;
    private String notes;
    private Long createdAt;
    private boolean deleted;
}
