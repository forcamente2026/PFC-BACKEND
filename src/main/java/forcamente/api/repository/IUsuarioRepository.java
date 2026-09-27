package forcamente.api.repository;

import forcamente.api.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IUsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByEmail(String email);

    List<UsuarioEntity> findByAnonimizacaoSolicitadaEmIsNotNullAndAnonimizadoEmIsNullOrderByAnonimizacaoSolicitadaEm();

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByCref(String cref);
}
