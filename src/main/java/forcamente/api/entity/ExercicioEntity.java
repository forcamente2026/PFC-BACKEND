package forcamente.api.entity;

import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exercicios")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExercicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, unique = true)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_muscular", nullable = false, length = 30)
    private GrupoMuscularEnum grupoMuscular;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    private NivelDificuldadeEnum nivel;

    @Column(name = "descricao_execucao", nullable = false, columnDefinition = "TEXT")
    private String descricaoExecucao;

    @Column(name = "erros_comuns", columnDefinition = "TEXT")
    private String errosComuns;

    @Column(name = "aquecimento_recomendado", columnDefinition = "TEXT")
    private String aquecimentoRecomendado;

    @Column(name = "equipamento")
    private String equipamento;

    @Column(name = "gif_url")
    private String gifUrl;

    @Column(name = "autor_id")
    private UUID autorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusAprovacaoEnum status;

    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;
}
