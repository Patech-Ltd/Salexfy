package org.PSmart.POS.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * A reusable role (Owner, Manager, Cashier, ...). {@code permissions} is stored as text,
 * e.g. "ADMIN=1,1;STOCK=1,0" meaning view,change per area. Assigning a profile to a user rewrites
 * that user's Role rows, so every existing permission check keeps working.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RoleProfileTable")
public class RoleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @Column(unique = true)
    private String name;

    private String description;

    @Column(length = 1000)
    private String permissions;

    private boolean builtIn;
    private boolean deleted;
}
