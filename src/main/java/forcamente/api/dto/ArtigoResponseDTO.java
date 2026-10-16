package forcamente.api.dto;

import forcamente.api.entity.enums.StatusAprovacaoEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ArtigoResponseDTO(
        UUID id,
        String titulo,
        OpcaoDTO categoria,
        String resumo,
        String fonte,
        String autoria,
        String tipoEstudo,
        Integer ano,
        Integer minutosLeitura,
        String autorNome,
        StatusAprovacaoEnum status,
        String motivoRejeicao,
        int aprovacoesDeProfessores,
        boolean podeEditar,
        boolean podeAprovar,
        List<String> professoresQueAprovaram,
        LocalDateTime criadoEm
) {
}
