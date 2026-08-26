package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
class CustomerSnapshotJpaEmbeddable {

    @Column(name = "customer_full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "customer_national_id", nullable = false, length = 20)
    private String nationalId;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String email;

    @Column(name = "customer_phone_number", nullable = false, length = 25)
    private String phoneNumber;

    @Column(name = "customer_postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "customer_street", nullable = false, length = 100)
    private String street;

    @Column(name = "customer_house_number", nullable = false, length = 10)
    private String houseNumber;

    @Column(name = "customer_complement", length = 50)
    private String complement;

    @Column(name = "customer_neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "customer_city", nullable = false, length = 100)
    private String city;

    @Column(name = "customer_state", nullable = false, length = 100)
    private String state;

    @Column(name = "customer_country", nullable = false, length = 100)
    private String country;
}
