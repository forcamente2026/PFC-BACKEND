package forcamente.api.repository;

import forcamente.api.entity.QuizAprovacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface IQuizAprovacaoRepository extends JpaRepository<QuizAprovacaoEntity, UUID> {

    boolean existsByQuizIdAndProfessorId(UUID quizId, UUID professorId);

    long countByQuizId(UUID quizId);

    List<QuizAprovacaoEntity> findByQuizIdIn(Collection<UUID> quizzesIds);

    void deleteByQuizId(UUID quizId);
}
