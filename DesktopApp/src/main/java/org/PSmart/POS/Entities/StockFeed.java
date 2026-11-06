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
public class StockFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    private Long doneOn;

    private Long doneBy;


    @OneToMany
    private List<StockFeedItem> stockFeedItems;


    private String comment;

    /** Audit trail for edits and cancellations (cancelled rows are kept, just hidden from totals). */
    private Long cancelledAt;
    private String cancelReason;
    private String cancelledBy;
    private Long editedAt;
    private String editedBy;

    private boolean deleted;


    public String getRec()
    {
        return "REC "+getUid();
    }



}
