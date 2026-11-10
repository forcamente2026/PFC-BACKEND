package forcamente.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "quiz_tentativas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuizTentativaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "acertos", nullable = false)
    private Integer acertos;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "feita_em", nullable = false)
    private LocalDateTime feitaEm;
}
