package org.PSmart.POS.Entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class WholeScale
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;


    private UUID itemId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @lombok.EqualsAndHashCode.Exclude @lombok.ToString.Exclude
    private Item item;

    private String name;

    private String Description;

    private Double items;

    private Double unitPrice;

    private boolean deleted;


    public Boolean isDiffent(WholeScale other)
    {
        if(other==null)
            return true;

        if(!name.equals(other.getName()))
            return true;
        if(!items.equals(other.getItems()))
            return true;
        if(!unitPrice.equals(other.getUnitPrice()))
            return true;

        return false;
    }

}
