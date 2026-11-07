package forcamente.api.repository;

import forcamente.api.entity.ExercicioEntity;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface IExercicioRepository extends JpaRepository<ExercicioEntity, UUID> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    List<ExercicioEntity> findByStatusInOrderByCriadoEmAsc(Collection<StatusAprovacaoEnum> status);

    @Query("""
            select e from ExercicioEntity e
            where (e.status in :visiveis
                   or (e.status = :rejeitado and e.autorId = :usuarioId))
              and (:grupoMuscular is null or e.grupoMuscular = :grupoMuscular)
            order by e.nome asc
            """)
    List<ExercicioEntity> buscarVisiveis(
            @Param("visiveis") Collection<StatusAprovacaoEnum> visiveis,
            @Param("rejeitado") StatusAprovacaoEnum rejeitado,
            @Param("usuarioId") UUID usuarioId,
            @Param("grupoMuscular") GrupoMuscularEnum grupoMuscular
    );
}
