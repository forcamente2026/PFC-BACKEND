package forcamente.api.repository;

import forcamente.api.entity.ArtigoAprovacaoEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IArtigoAprovacaoRepository  extends JpaRepository<ArtigoAprovacaoEntity, UUID> {
    void deleteByArtigoId(UUID artigoId);

    boolean existsByArtigoIdAndProfessorId(UUID artigoId, UUID professorId);

    long countByArtigoId(UUID artigoId);

    List<ArtigoAprovacaoEntity> findByArtigoIdIn(Collection<UUID> artigosIds);
}
