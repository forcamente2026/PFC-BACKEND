package forcamente.api.repository;

import forcamente.api.entity.ArtigoEntity;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface IArtigoRepository extends JpaRepository<ArtigoEntity, UUID> {
    boolean existsByFonte(String fonte);

    boolean existsByFonteAndIdNot(String fonte, UUID id);

    List<ArtigoEntity> findByStatusInOrderByCriadoEmAsc(Collection<StatusAprovacaoEnum> status);

    @Query("""
            select a from ArtigoEntity a
            where (a.status in :visiveis
                   or (a.status = :rejeitado and a.autorId = :usuarioId))
              and (:status is null or a.status = :status)
            order by a.criadoEm desc
            """)
    List<ArtigoEntity> buscarVisiveis(
            @Param("visiveis")Collection<StatusAprovacaoEnum> visiveis,
            @Param("rejeitado") StatusAprovacaoEnum rejeitado,
            @Param("usuarioId") UUID usuarioId,
            @Param("status") StatusAprovacaoEnum status
            );
}
