package io.github.jvlealc.marketsphere.customers.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.Objects;

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
@Getter
@Setter
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

    @OneToOne(mappedBy = "customer")
    private Address address;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Customer other = (Customer) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id != null
                ? Objects.hashCode(this.id)
                : getClass().hashCode();
    }
}
