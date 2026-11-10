package forcamente.api.entity;


import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documentos_legais",
uniqueConstraints = @UniqueConstraint(columnNames = {"tipo","versao"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentoLegalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name="tipo", nullable = false, length = 30)
    private TipoDocumentoLegalEnum tipo;

    @Column(name="versao", nullable = false, length = 10)
    private String versao;

    @Column(name = "texto", nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name="vigente_desde", nullable = false)
    private LocalDateTime vigenteDesde;


}
