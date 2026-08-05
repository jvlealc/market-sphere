package io.github.jvlealc.marketsphere.billing.application.model.order;

import io.github.jvlealc.marketsphere.billing.application.exception.InvalidOrderPaidSnapshotException;

/**
 * Endereço de entrega, como estava no momento do pagamento.
 * <p>
 * {@code complement} e {@code neighborhood} são opcionais porque são opcionais na origem: as duas colunas
 * são nullable em {@code customers}. Tornar a validação de borda mais estrita que o contrato de quem
 * publica custa caro aqui, porque {@link InvalidOrderPaidSnapshotException} é terminal — o pedido vai à
 * DLT sem gerar nota. Um endereço sem complemento é faturável; um sem cidade, não.
 * <p>
 * Os campos opcionais são normalizados de branco para {@code null}, para que exista uma representação só
 * de "não informado".
 */
public record OrderPaidAddress(
        String postalCode,
        String street,
        String houseNumber,
        String complement,
        String neighborhood,
        String city,
        String state,
        String country
) {
    public OrderPaidAddress {
        if (postalCode == null || postalCode.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("Postal code is required");
        }
        if (street == null || street.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("Street is required");
        }
        if (houseNumber == null || houseNumber.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("House number is required");
        }
        if (city == null || city.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("City is required");
        }
        if (state == null || state.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("State is required");
        }
        if (country == null || country.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("Country is required");
        }

        complement = normalizeOptional(complement);
        neighborhood = normalizeOptional(neighborhood);
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
