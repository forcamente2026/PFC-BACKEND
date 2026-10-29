package forcamente.api.repository;

import forcamente.api.entity.QuizEntity;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface IQuizRepository extends JpaRepository<QuizEntity, UUID> {

    List<QuizEntity> findByStatusInOrderByCriadoEmAsc(Collection<StatusAprovacaoEnum> status);

    @Query("""
            select q from QuizEntity q
            where q.status in :visiveis
               or (q.status = :rejeitado and q.autorId = :usuarioId)
            order by q.criadoEm desc
            """)
    List<QuizEntity> buscarVisiveis(
            @Param("visiveis") Collection<StatusAprovacaoEnum> visiveis,
            @Param("rejeitado") StatusAprovacaoEnum rejeitado,
            @Param("usuarioId") UUID usuarioId
    );

    @Query("""
            select q from QuizEntity q join q.artigoIds artigoId
            where artigoId = :artigoId
              and q.status in :publicados
            """)
    List<QuizEntity> buscarPorArtigoIndicado(
            @Param("artigoId") UUID artigoId,
            @Param("publicados") Collection<StatusAprovacaoEnum> publicados
    );
}
