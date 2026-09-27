package forcamente.api.repository;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("IUsuarioRepository - armadilha do parametro nulo no existsByCpf")
class UsuarioRepositoryCpfNuloTest {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Test
    @DisplayName("existsByCpf(null) devolve true quando alguem nao tem CPF: o Spring Data traduz nulo em IS NULL")
    void existsByCpfComNuloEncontraQuemNaoTemCpf() {
        usuarioRepository.saveAndFlush(umAlunoSemCpf());

        assertThat(usuarioRepository.existsByCpf(null))
                .as("nulo vira 'cpf is null' na consulta, e nao 'cpf = null'")
                .isTrue();

        assertThat(usuarioRepository.existsByCpf("00000000000")).isFalse();
    }

    private UsuarioEntity umAlunoSemCpf() {
        var aluno = new UsuarioEntity();
        aluno.setNomeCompleto("Aluno Sem CPF");
        aluno.setEmail("aluno.sem.cpf." + UUID.randomUUID() + "@umc.br");
        aluno.setSenhaHash("$2a$10$hash");
        aluno.setPapel(PapelUsuarioEnum.ALUNO);
        aluno.setDataNascimento(LocalDate.of(2000, 1, 1));
        aluno.setAtivo(true);
        aluno.setCriadoEm(LocalDateTime.now());
        return aluno;
    }
}
