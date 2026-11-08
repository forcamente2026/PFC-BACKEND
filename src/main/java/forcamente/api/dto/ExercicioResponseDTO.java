package forcamente.api.dto;

import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExercicioResponseDTO(
        UUID id,
        String nome,
        GrupoMuscularEnum grupoMuscular,
        NivelDificuldadeEnum nivel,
        String descricaoExecucao,
        String errosComuns,
        String aquecimentoRecomendado,
        String equipamento,
        String gifUrl,
        String videoYoutubeId,
        String autorNome,
        StatusAprovacaoEnum status,
        String motivoRejeicao,
        int aprovacoesDeProfessores,
        int aprovacoesNecessarias,
        boolean podeEditar,
        boolean podeAprovar,
        List<String> professoresQueAprovaram,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
