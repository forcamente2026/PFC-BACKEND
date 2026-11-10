package forcamente.api.dto;

public record GastoEnergeticoResponseDTO(
        Integer tmb,
        Integer gastoEnergeticoTotal,
        Double fatorAtividadeAplicado,
        String formulaUtilizada
) {
}
