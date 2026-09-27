package forcamente.api.service;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.AnonimizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnonimizacaoService - solicitacao e anonimizacao irreversivel")
class AnonimizacaoServiceTest {

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IAuditoriaService auditoriaService;

    private AnonimizacaoService anonimizacaoService;

    private UsuarioEntity usuario;

    @BeforeEach
    void configurar() {
        anonimizacaoService = new AnonimizacaoService(
                usuarioRepository, new BCryptPasswordEncoder(), RELOGIO, auditoriaService);
        usuario = umProfessor();
    }

    @Test
    @DisplayName("solicitar marca a data do pedido")
    void deveMarcarSolicitacao() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        anonimizacaoService.solicitar(usuario.getId());

        assertThat(usuario.getAnonimizacaoSolicitadaEm()).isEqualTo(LocalDateTime.now(RELOGIO));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("solicitar duas vezes nao muda a data da primeira")
    void solicitarDuasVezesMantemAPrimeiraData() {
        var primeira = LocalDateTime.of(2026, 9, 1, 8, 0);
        usuario.setAnonimizacaoSolicitadaEm(primeira);
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        anonimizacaoService.solicitar(usuario.getId());

        assertThat(usuario.getAnonimizacaoSolicitadaEm()).isEqualTo(primeira);
    }

    @Test
    @DisplayName("deve apagar os dados que identificam e preservar o registro do aceite")
    void deveAnonimizarPreservandoOAceite() {
        usuario.setAnonimizacaoSolicitadaEm(LocalDateTime.of(2026, 9, 1, 8, 0));
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        var idOriginal = usuario.getId();
        var criadoEmOriginal = usuario.getCriadoEm();
        var aceiteOriginal = usuario.getAceitouTermosUsoEm();
        var hashOriginal = usuario.getSenhaHash();

        anonimizacaoService.anonimizar(idOriginal);

        assertThat(usuario.getNomeCompleto()).isEqualTo("Usuario anonimizado");
        assertThat(usuario.getEmail()).isEqualTo("anonimizado-" + idOriginal + "@forcamente.invalid");
        assertThat(usuario.getDataNascimento()).isNull();
        assertThat(usuario.getCref()).isNull();
        assertThat(usuario.getFormacao()).isNull();
        assertThat(usuario.getCidade()).isNull();
        assertThat(usuario.isAtivo()).isFalse();
        assertThat(usuario.getAnonimizadoEm()).isEqualTo(LocalDateTime.now(RELOGIO));

        assertThat(usuario.getSenhaHash()).isNotEqualTo(hashOriginal).startsWith("$2");
        assertThat(usuario.getId()).isEqualTo(idOriginal);
        assertThat(usuario.getPapel()).isEqualTo(PapelUsuarioEnum.PROFESSOR);
        assertThat(usuario.getCriadoEm()).isEqualTo(criadoEmOriginal);
        assertThat(usuario.getAceitouTermosUsoEm()).isEqualTo(aceiteOriginal);
        assertThat(usuario.getVersaoTermosUso()).isEqualTo("1.0");
    }

    @Test
    @DisplayName("nao deve anonimizar quem nao solicitou")
    void naoDeveAnonimizarSemSolicitacao() {
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> anonimizacaoService.anonimizar(usuario.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("nao solicitou");
    }

    @Test
    @DisplayName("nao deve anonimizar duas vezes")
    void naoDeveAnonimizarDuasVezes() {
        usuario.setAnonimizacaoSolicitadaEm(LocalDateTime.of(2026, 9, 1, 8, 0));
        usuario.setAnonimizadoEm(LocalDateTime.of(2026, 9, 2, 8, 0));
        when(usuarioRepository.findById(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> anonimizacaoService.anonimizar(usuario.getId()))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("ja foi anonimizada");
    }

    @Test
    @DisplayName("deve listar apenas quem solicitou e ainda nao foi anonimizado")
    void deveListarPendentes() {
        usuario.setAnonimizacaoSolicitadaEm(LocalDateTime.of(2026, 9, 1, 8, 0));
        when(usuarioRepository
                .findByAnonimizacaoSolicitadaEmIsNotNullAndAnonimizadoEmIsNullOrderByAnonimizacaoSolicitadaEm())
                .thenReturn(java.util.List.of(usuario));

        var resultado = anonimizacaoService.listarPendentes();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().email()).isEqualTo("maria@umc.br");
        assertThat(resultado.getFirst().solicitadaEm()).isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 0));
    }

    private UsuarioEntity umProfessor() {
        var entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNomeCompleto("Maria Souza");
        entity.setDataNascimento(LocalDate.of(1990, 5, 20));
        entity.setEmail("maria@umc.br");
        entity.setSenhaHash("$2a$10$hashOriginal");
        entity.setPapel(PapelUsuarioEnum.PROFESSOR);
        entity.setCref("123456-G/SP");
        entity.setFormacao(FormacaoEnum.BACHARELADO);
        entity.setCidade("Mogi das Cruzes");
        entity.setEstado("SP");
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.of(2026, 1, 10, 9, 0));
        entity.setAceitouTermosUsoEm(LocalDateTime.of(2026, 1, 10, 9, 0));
        entity.setVersaoTermosUso("1.0");
        return entity;
    }
}