package org.PSmart.POS.Entities;

import lombok.Data;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "UserTable")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uid;

    @Column(unique = true)
    private String username;


    private String password;


    private String phone;

    private String fullname;

    private boolean deleted;

    /** Name of the RoleProfile assigned to this user ("Custom" when permissions were hand-edited). */
    private String roleProfile;

    /** Commission rate (%) for this user; null = use the store default from Settings. */
    private Double commissionRate;

    public String getSmallName()
    {
        String shortName=fullname;
        try{
            shortName=fullname.split(" ")[0];
        }catch (Exception ett)
        {

        }
        return shortName;
    }

    @ManyToMany(mappedBy = "user")
    private List<Role> roles;
}
