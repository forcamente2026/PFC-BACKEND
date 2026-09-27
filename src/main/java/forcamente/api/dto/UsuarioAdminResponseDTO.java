package forcamente.api.dto;

import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioAdminResponseDTO(
        UUID id,
        String nomeCompleto,
        String email,
        PapelUsuarioEnum papel,
        LocalDate dataNascimento,
        String cref,
        FormacaoEnum formacao,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime anonimizacaoSolicitadaEm,
        LocalDateTime anonimizadoEm
) {
}
