package forcamente.api.entity;

import forcamente.api.entity.enums.CategoriaArtigoEnum;
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
@Table(name = "artigos")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArtigoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private CategoriaArtigoEnum categoria;

    @Column(name = "resumo", nullable = false, columnDefinition = "TEXT")
    private String resumo;

    @Column(name = "fonte", nullable = false, unique = true, length = 500)
    private String fonte;

    @Column(name = "autoria")
    private String autoria;

    @Column(name = "tipo_estudo")
    private String tipoEstudo;

    @Column(name = "ano")
    private Integer ano;

    @Column(name = "minutos_leitura")
    private Integer minutosLeitura;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusAprovacaoEnum status;

    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}