package forcamente.api.dto;

public record DesempenhoQuizResponseDTO(
        String quiz,
        long tentativas,
        int notaMedia
) {
}
