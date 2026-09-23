package forcamente.api.repository;

import forcamente.api.entity.DocumentoLegalEntity;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IDocumentoLegalRepository extends JpaRepository<DocumentoLegalEntity, UUID> {

    Optional<DocumentoLegalEntity> findFirstByTipoOrderByVigenteDesdeDesc(TipoDocumentoLegalEnum tipo);

    boolean existsByTipo(TipoDocumentoLegalEnum tipo);
}