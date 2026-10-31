package forcamente.api.repository;

import forcamente.api.entity.QuizTentativaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface IQuizTentativaRepository extends JpaRepository<QuizTentativaEntity, UUID> {

    List<QuizTentativaEntity> findByUsuarioIdAndQuizIdIn(UUID usuarioId, Collection<UUID> quizzesIds);

    void deleteByUsuarioId(UUID usuarioId);
}
