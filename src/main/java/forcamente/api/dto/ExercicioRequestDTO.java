package forcamente.api.dto;

import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExercicioRequestDTO(

        @NotBlank(message = "O nome do exercicio e obrigatorio")
        @Size(max = 120, message = "O nome deve ter no maximo 120 caracteres")
        String nome,

        @NotNull(message = "O grupo muscular e obrigatorio")
        GrupoMuscularEnum grupoMuscular,

        @NotNull(message = "O nivel de dificuldade e obrigatorio")
        NivelDificuldadeEnum nivel,

        @NotBlank(message = "A descricao da execucao e obrigatoria")
        @Size(max = 1000, message = "A descrição da execução deve ter no máximo 1000 caracteres")
        String descricaoExecucao,

        @Size(max = 500, message = "Os erros comuns devem ter no máximo 500 caracteres")
        String errosComuns,

        @Size(max = 500, message = "O aquecimento recomendado deve ter no máximo 500 caracteres")
        String aquecimentoRecomendado,

        @Size(max = 100, message = "O equipamento deve ter no máximo 100 caracteres")
        String equipamento,

        @Size(max = 255, message = "O link do GIF deve ter no máximo 255 caracteres")
        String gifUrl,

        @Pattern(regexp = "[A-Za-z0-9_-]{11}",
                message = "Informe apenas o id do video do YouTube, com 11 caracteres")
        String videoYoutubeId
) {
        public ExercicioRequestDTO{
                nome = nome == null ? null : nome.trim();
                videoYoutubeId = videoYoutubeId == null || videoYoutubeId.isBlank()
                        ? null
                        : videoYoutubeId.trim();
        }
}
