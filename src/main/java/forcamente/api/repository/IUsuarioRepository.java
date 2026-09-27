package forcamente.api.repository;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByEmail(String email);

    List<UsuarioEntity> findByAnonimizacaoSolicitadaEmIsNotNullAndAnonimizadoEmIsNullOrderByAnonimizacaoSolicitadaEm();

    boolean existsByEmail(String email);

    boolean existsByCref(String cref);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByCrefAndIdNot(String cref, UUID id);

    @Query("""
            select u from UsuarioEntity u
            where (:papel is null or u.papel = :papel)
              and (:ativo is null or u.ativo = :ativo)
              and (:busca is null
                   or lower(u.nomeCompleto) like lower(concat('%', cast(:busca as string), '%'))
                   or lower(u.email) like lower(concat('%', cast(:busca as string), '%')))
            order by u.nomeCompleto
            """)
    Page<UsuarioEntity> buscarComFiltros(
            @Param("papel") PapelUsuarioEnum papel,
            @Param("ativo") Boolean ativo,
            @Param("busca") String busca,
            Pageable paginacao);
}
