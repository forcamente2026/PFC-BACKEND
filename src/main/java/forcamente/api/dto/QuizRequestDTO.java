package forcamente.api.dto;

import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.validation.SemMarcacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record QuizRequestDTO(

        @NotBlank(message = "Informe o título")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        @SemMarcacao
        String titulo,

        @NotBlank(message = "Informe a descrição")
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        @SemMarcacao
        String descricao,

        @NotNull(message = "Informe a categoria")
        CategoriaArtigoEnum categoria,

        @NotNull(message = "Informe o nível")
        NivelDificuldadeEnum nivel,

        @Size(max = 10, message = "Indique no máximo 10 artigos")
        List<@NotNull(message = "Informe o artigo") UUID> artigos,

        @NotNull(message = "Inclua as perguntas")
        @Size(min = 3, max = 10, message = "O quiz deve ter de 3 a 10 perguntas")
        @Valid
        List<@NotNull(message = "Informe a pergunta") QuestaoRequestDTO> questoes
) {
}
