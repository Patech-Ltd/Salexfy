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
@Table(name = "ExpenseItemTable")
public class ExpenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private Expense expense;

    private String description;

    private Double amount;

    private boolean deleted;
}
