package forcamente.api.dto;

public record ConsumoAguaResponseDTO(
        Integer metaMl,
        Integer totalConsumidoMl,
        Integer restanteMl,
        Integer percentualDaMeta
) {
}
