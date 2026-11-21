package forcamente.api.repository;

import forcamente.api.cifra.CifraDeEmail;
import forcamente.api.entity.UsuarioEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class UsuarioPorEmailRepositoryImpl implements UsuarioPorEmailRepository {

    private final CifraDeEmail cifraDeEmail;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UsuarioEntity> findByEmail(String email) {
        return entityManager
                .createQuery("select u from UsuarioEntity u where u.emailHash = :indice", UsuarioEntity.class)
                .setParameter("indice", cifraDeEmail.indice(email))
                .getResultStream()
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return findByEmail(email)
                .filter(usuario -> !usuario.getId().equals(id))
                .isPresent();
    }
}
