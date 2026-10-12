package forcamente.api.dto;

public record HidratacaoResponseDTO(
        Integer mlPorDia,
        Double litrosPorDia,
        Integer baseCorporalMl,
        Integer adicionalTreinoMl
) {
}
