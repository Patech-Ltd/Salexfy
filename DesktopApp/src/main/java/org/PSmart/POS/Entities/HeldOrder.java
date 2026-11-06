package org.PSmart.POS.Entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

/**
 * A held (parked) sale that is persisted so it survives app restarts. Restores
 * the cart snapshot item for item. Capped at {@code HelderOrderRepository.MAX_HELD}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "HeldOrderTable")
public class HeldOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private Long createdAt;

    @ManyToOne
    private Customer customer;

    private String note;

    private Double subtotal;

    private Double discount;

    private Double vatAmount;

    private Double total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<HeldOrderItem> items;

    private boolean deleted;
}