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
@Table(name = "CustomerTable")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private String fullname;

    private String phone;

    private String email;

    private String address;

    private Double creditLimit;

    private Double currentBalance;

    private String notes;

    /** Loyalty points balance (null = 0). */
    private Double loyaltyPoints;

    /** Lifetime spend recorded by the till (null = 0). */
    private Double totalSpent;

    private boolean deleted;

    public String toString() {
        return getFullname();
    }

    public String getSearchString() {
        return (fullname + " " + phone + " " + email).toLowerCase();
    }
}
