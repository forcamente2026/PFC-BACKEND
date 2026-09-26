package forcamente.api.service;

import forcamente.api.config.SegurancaConfig;
import forcamente.api.dto.EsqueciSenhaRequestDTO;
import forcamente.api.dto.LoginRequestDTO;
import forcamente.api.dto.RedefinirSenhaRequestDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.TipoCodigoEnum;
import forcamente.api.exception.CredenciaisInvalidasException;
import forcamente.api.exception.LimiteDeTentativasException;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - login com e-mail e senha")
class AuthServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-trinta-e-dois-caracteres";
    private static final long EXPIRACAO_MINUTOS = 120;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ICodigoVerificacaoService codigoVerificacaoService;

    @Mock
    private IEmailService emailService;

    private final SegurancaConfig segurancaConfig = new SegurancaConfig();

    private AuthService authService;

    @BeforeEach
    void configurar() {
        authService = new AuthService(
                usuarioRepository, passwordEncoder, segurancaConfig.jwtEncoder(SEGREDO),codigoVerificacaoService, emailService, EXPIRACAO_MINUTOS);
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

    @Test
    @DisplayName("deve gerar codigo e enviar e-mail quando o e-mail existe")
    void deveEnviarCodigoDeRedefinicao() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(codigoVerificacaoService.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA))
                .thenReturn("1234");

        authService.esqueciSenha(new EsqueciSenhaRequestDTO("  Joao@UMC.br "));

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarAssincrono(eq("joao@umc.br"), anyString(), texto.capture());
        assertThat(texto.getValue()).contains("1234").contains("15 minutos");
    }

    @Test
    @DisplayName("e-mail inexistente nao gera codigo nem envia nada, e nao lanca excecao")
    void naoDeveRevelarEmailInexistente() {
        when(usuarioRepository.findByEmail("ninguem@umc.br")).thenReturn(Optional.empty());

        authService.esqueciSenha(new EsqueciSenhaRequestDTO("ninguem@umc.br"));

        verify(codigoVerificacaoService, never()).gerarCodigo(any(), any());
        verify(emailService, never()).enviarAssincrono(any(), any(), any());
    }

    @Test
    @DisplayName("usuario inativo recebe a mesma resposta, sem e-mail")
    void naoDeveEnviarParaUsuarioInativo() {
        var inativo = umUsuarioAtivo();
        inativo.setAtivo(false);
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(inativo));

        authService.esqueciSenha(new EsqueciSenhaRequestDTO("joao@umc.br"));

        verify(emailService, never()).enviarAssincrono(any(), any(), any());
    }

    @Test
    @DisplayName("pedido bloqueado por limite nao vaza o 429 nem envia e-mail")
    void bloqueioNaoVazaNoPedido() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(codigoVerificacaoService.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA))
                .thenThrow(new LimiteDeTentativasException("Limite diario"));

        authService.esqueciSenha(new EsqueciSenhaRequestDTO("joao@umc.br"));

        verify(emailService, never()).enviarAssincrono(any(), any(), any());
    }

    @Test
    @DisplayName("deve trocar o hash da senha quando o codigo e valido")
    void deveRedefinirSenha() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("NovaSenha@1")).thenReturn("$2a$10$novo");

        authService.redefinirSenha(
                new RedefinirSenhaRequestDTO("joao@umc.br", "1234", "NovaSenha@1"));

        verify(codigoVerificacaoService)
                .validarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA, "1234");
        assertThat(usuario.getSenhaHash()).isEqualTo("$2a$10$novo");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("redefinicao com e-mail inexistente devolve a mesma mensagem de codigo invalido")
    void naoDeveRedefinirComEmailInexistente() {
        when(usuarioRepository.findByEmail("ninguem@umc.br")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.redefinirSenha(
                new RedefinirSenhaRequestDTO("ninguem@umc.br", "1234", "NovaSenha@1")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Codigo invalido ou expirado");

        verify(codigoVerificacaoService, never()).validarCodigo(any(), any(), any());
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