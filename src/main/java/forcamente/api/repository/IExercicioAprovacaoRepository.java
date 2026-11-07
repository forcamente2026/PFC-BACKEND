package forcamente.api.repository;

import forcamente.api.entity.ExercicioAprovacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface IExercicioAprovacaoRepository extends JpaRepository<ExercicioAprovacaoEntity, UUID> {

    boolean existsByExercicioIdAndProfessorId(UUID exercicioId, UUID professorId);

    long countByExercicioId(UUID exercicioId);

    List<ExercicioAprovacaoEntity> findByExercicioIdIn(Collection<UUID> exerciciosIds);

    void deleteByExercicioId(UUID exercicioId);
}
