package forcamente.api.service;

import forcamente.api.config.SegurancaConfig;
import forcamente.api.dto.LoginRequestDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.exception.CredenciaisInvalidasException;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - login com e-mail e senha")
class AuthServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-trinta-e-dois-caracteres";
    private static final long EXPIRACAO_MINUTOS = 120;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final SegurancaConfig segurancaConfig = new SegurancaConfig();

    private AuthService authService;

    @BeforeEach
    void configurar() {
        authService = new AuthService(
                usuarioRepository, passwordEncoder, segurancaConfig.jwtEncoder(SEGREDO), EXPIRACAO_MINUTOS);
    }

    @Test
    @DisplayName("deve autenticar, devolver o nome e um token com id, papel e expiracao")
    void deveAutenticarEDevolverToken() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", "$2a$10$hash")).thenReturn(true);

        var resposta = authService.login(new LoginRequestDTO("  Joao@UMC.br ", "Senha@123"));

        assertThat(resposta.nomeCompleto()).isEqualTo("Joao da Silva");
        assertThat(resposta.papel()).isEqualTo(PapelUsuarioEnum.ALUNO);

        var jwt = segurancaConfig.jwtDecoder(SEGREDO).decode(resposta.token());
        assertThat(jwt.getSubject()).isEqualTo(usuario.getId().toString());
        assertThat(jwt.getClaimAsString("papel")).isEqualTo("ALUNO");
        assertThat(jwt.getExpiresAt()).isBetween(
                Instant.now().plus(EXPIRACAO_MINUTOS - 1, ChronoUnit.MINUTES),
                Instant.now().plus(EXPIRACAO_MINUTOS + 1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("deve recusar e-mail inexistente sem consultar a senha")
    void deveRecusarEmailInexistente() {
        when(usuarioRepository.findByEmail("ninguem@umc.br")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("ninguem@umc.br", "Senha@123")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha invalidos");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("deve recusar senha errada com a mesma mensagem")
    void deveRecusarSenhaErrada() {
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(umUsuarioAtivo()));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("joao@umc.br", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha invalidos");
    }

    @Test
    @DisplayName("deve recusar usuario inativo sem consultar a senha")
    void deveRecusarUsuarioInativo() {
        var inativo = umUsuarioAtivo();
        inativo.setAtivo(false);
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("joao@umc.br", "Senha@123")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha invalidos");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    private UsuarioEntity umUsuarioAtivo() {
        var entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNomeCompleto("Joao da Silva");
        entity.setCpf("12345678901");
        entity.setDataNascimento(LocalDate.of(2000, 1, 1));
        entity.setEmail("joao@umc.br");
        entity.setSenhaHash("$2a$10$hash");
        entity.setPapel(PapelUsuarioEnum.ALUNO);
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.now());
        return entity;
    }
}