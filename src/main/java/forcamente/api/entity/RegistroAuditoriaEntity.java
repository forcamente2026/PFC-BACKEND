package forcamente.api.entity;

import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "registros_auditoria",
        indexes = @Index(name = "idx_auditoria_ocorrido_em", columnList = "ocorrido_em"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegistroAuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 30)
    private AcaoAuditoriaEnum acao;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurso_tipo", length = 20)
    private RecursoAuditoriaEnum recursoTipo;

    @Column(name = "recurso_id")
    private UUID recursoId;

    @Column(name = "usuario_id")
    private UUID usuarioId;
}
