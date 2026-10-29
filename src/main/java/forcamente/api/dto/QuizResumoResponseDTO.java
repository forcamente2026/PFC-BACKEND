package forcamente.api.dto;

import forcamente.api.entity.enums.StatusAprovacaoEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuizResumoResponseDTO(
        UUID id,
        String titulo,
        String descricao,
        OpcaoDTO categoria,
        OpcaoDTO nivel,
        String autorNome,
        StatusAprovacaoEnum status,
        String motivoRejeicao,
        int aprovacoesDeProfessores,
        int aprovacoesNecessarias,
        boolean podeEditar,
        boolean podeAprovar,
        List<String> professoresQueAprovaram,
        int totalQuestoes,
        int notaMinima,
        NotaDTO melhorTentativa,
        LocalDateTime criadoEm
) {
}
