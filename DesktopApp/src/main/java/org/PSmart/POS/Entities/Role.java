package org.PSmart.POS.Entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private User user;

    private RoleType roleType;

    private Boolean view;

    private Boolean change;


}
