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
@Table(name = "HeldOrderItemTable")
public class HeldOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @ManyToOne
    private HeldOrder order;

    @ManyToOne
    private Item item;

    private Double quantity;

    private Double unitPrice;

    private Double buyingPrice;

    private Double discount;

    private boolean deleted;
}