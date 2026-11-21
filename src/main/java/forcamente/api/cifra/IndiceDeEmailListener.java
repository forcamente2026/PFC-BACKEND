package forcamente.api.cifra;

import forcamente.api.entity.UsuarioEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IndiceDeEmailListener {

    private final CifraDeEmail cifraDeEmail;

    @PrePersist
    @PreUpdate
    public void atualizarIndice(UsuarioEntity usuario) {
        usuario.setEmailHash(cifraDeEmail.indice(usuario.getEmail()));
    }
}
