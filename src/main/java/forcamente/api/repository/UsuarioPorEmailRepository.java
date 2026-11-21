package forcamente.api.repository;

import forcamente.api.entity.UsuarioEntity;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioPorEmailRepository {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
