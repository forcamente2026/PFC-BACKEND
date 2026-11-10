package forcamente.api.dto;

public record EnderecoResponseDTO(
        String cep,
        String logradouro,
        String bairro,
        String cidade,
        String estado
) {
}
