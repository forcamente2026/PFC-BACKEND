package forcamente.api.dto;

import forcamente.api.entity.enums.StatusAprovacaoEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuizDetalheResponseDTO(
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
        LocalDateTime criadoEm,
        boolean gabarito,
        List<ArtigoSugeridoDTO> artigos,
        List<QuestaoResponseDTO> questoes
) {

    public static QuizDetalheResponseDTO de(QuizResumoResponseDTO resumo,
                                            boolean gabarito,
                                            List<ArtigoSugeridoDTO> artigos,
                                            List<QuestaoResponseDTO> questoes) {

        return new QuizDetalheResponseDTO(
                resumo.id(),
                resumo.titulo(),
                resumo.descricao(),
                resumo.categoria(),
                resumo.nivel(),
                resumo.autorNome(),
                resumo.status(),
                resumo.motivoRejeicao(),
                resumo.aprovacoesDeProfessores(),
                resumo.aprovacoesNecessarias(),
                resumo.podeEditar(),
                resumo.podeAprovar(),
                resumo.professoresQueAprovaram(),
                resumo.totalQuestoes(),
                resumo.notaMinima(),
                resumo.melhorTentativa(),
                resumo.criadoEm(),
                gabarito,
                artigos,
                questoes
        );
    }
}
