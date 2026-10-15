package forcamente.api.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "artigo_aprovacoes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArtigoAprovacaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false,nullable = false)
    private UUID id;

    @Column(name = "artigo_id", nullable = false)
    private UUID artigoId;

    @Column(name = "professor_id", nullable = false)
    private UUID professorId;

    @Column(name = "aprovado_em", nullable = false)
    private LocalDateTime aprovadoEm;
}
