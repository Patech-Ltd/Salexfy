package org.PSmart.POS.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Commission paid out to a staff member. */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "CommissionPayoutTable")
public class CommissionPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private UUID userUid;
    private String userName;
    private Double amount;
    private String method;
    private String notes;
    private String paidByName;
    private Long createdAt;
    private boolean deleted;
}
