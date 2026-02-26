package io.github.jvlealc.marketsphere.customers.model;

import io.github.jvlealc.marketsphere.customers.model.vo.Address;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "customers",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "national_id"),
            @UniqueConstraint(columnNames = "email")
        })
@SQLDelete(sql = "UPDATE customers SET active = false WHERE id = ?")
@SQLRestriction("active = true")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "national_id", nullable = false, unique = true, length = 11)
    private String nationalId;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 25)
    private String phoneNumber;

    @Embedded
    private Address addressVo;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;
}
