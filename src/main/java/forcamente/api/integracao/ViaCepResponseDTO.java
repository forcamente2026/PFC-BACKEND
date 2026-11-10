package forcamente.api.integracao;

public record ViaCepResponseDTO(
        String cep,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        Boolean erro
) {
}
