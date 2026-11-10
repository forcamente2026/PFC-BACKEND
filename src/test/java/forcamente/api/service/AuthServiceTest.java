package forcamente.api.service;

import forcamente.api.config.SegurancaConfig;
import forcamente.api.dto.EsqueciSenhaRequestDTO;
import forcamente.api.dto.LoginRequestDTO;
import forcamente.api.dto.RedefinirSenhaRequestDTO;
import forcamente.api.dto.VerificarCodigoRequestDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;
import java.time.ZoneId;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import static org.mockito.Mockito.times;

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

    @Mock
    private IAuditoriaService auditoriaService;

    private final SegurancaConfig segurancaConfig = new SegurancaConfig();

    private AuthService authService;

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 10, 12, 0);

    private RelogioAjustavel relogio;

    private static final String MENSAGEM_LOGIN_INVALIDO =
            "E-mail ou senha inválidos. Se não lembra a senha, use \"esqueci minha senha\".";

    @BeforeEach
    void configurar() {
        relogio = new RelogioAjustavel(INICIO);
        authService = new AuthService(
                usuarioRepository, passwordEncoder, segurancaConfig.jwtEncoder(SEGREDO),
                codigoVerificacaoService, emailService, auditoriaService, relogio, EXPIRACAO_MINUTOS);
    }

    @Test
    @DisplayName("login valida a senha, gera codigo, envia e-mail sincrono e nao devolve token")
    void loginGeraCodigoEEnviaEmail() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", "$2a$10$hash")).thenReturn(true);
        when(codigoVerificacaoService.gerarCodigo(usuario, TipoCodigoEnum.MFA)).thenReturn("1234");
        when(codigoVerificacaoService.validadeEmSegundos(TipoCodigoEnum.MFA)).thenReturn(120);


        var resposta = authService.login(new LoginRequestDTO("  Joao@UMC.br ", "Senha@123"));

        assertThat(resposta.mfaNecessario()).isTrue();
        assertThat(resposta.expiraEmSegundos()).isEqualTo(120);

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviar(eq("joao@umc.br"), anyString(), texto.capture());
        assertThat(texto.getValue()).contains("1234");
        verify(emailService, never()).enviarAssincrono(any(), any(), any());

    }

    @Test
    @DisplayName("codigo correto devolve token com id e papel, e o nome do usuario")
    void verificarCodigoDevolveToken() {
        var usuario = umUsuarioAtivo();

        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));

        var resposta = authService.verificarCodigo(
                new VerificarCodigoRequestDTO("  Joao@UMC.br ", " 1234 "));

        verify(codigoVerificacaoService).validarCodigo(usuario, TipoCodigoEnum.MFA, "1234");
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
                .hasMessage(MENSAGEM_LOGIN_INVALIDO);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(codigoVerificacaoService, never()).gerarCodigo(any(), any());
    }

    @Test
    @DisplayName("deve recusar senha errada com a mesma mensagem")
    void deveRecusarSenhaErrada() {
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(umUsuarioAtivo()));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("joao@umc.br", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage(MENSAGEM_LOGIN_INVALIDO);
        verify(codigoVerificacaoService, never()).gerarCodigo(any(), any());
    }

    @Test
    @DisplayName("deve recusar usuario inativo sem consultar a senha")
    void deveRecusarUsuarioInativo() {
        var inativo = umUsuarioAtivo();
        inativo.setAtivo(false);
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("joao@umc.br", "Senha@123")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage(MENSAGEM_LOGIN_INVALIDO);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(codigoVerificacaoService, never()).gerarCodigo(any(), any());
    }
    @Test
    @DisplayName("tres falhas de senha nao suspendem a conta")
    void tresFalhasNaoSuspendem() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        falharSenha(3);

        assertThat(usuario.getSenhaErrosConsecutivos()).isEqualTo(3);
        assertThat(usuario.getSenhaBloqueadoAte()).isNull();
        assertThat(usuario.getSenhaPausaOcorrencias()).isNull();
    }

    @Test
    @DisplayName("a quarta falha suspende a conta por um minuto")
    void quartaFalhaSuspendePorUmMinuto() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        falharSenha(4);

        assertThat(usuario.getSenhaBloqueadoAte()).isEqualTo(INICIO.plusMinutes(1));
        assertThat(usuario.getSenhaPausaOcorrencias()).isEqualTo(1);
        assertThat(usuario.getSenhaErrosConsecutivos()).isZero();
    }

    @Test
    @DisplayName("tentativa durante a suspensao e recusada sem conferir a senha")
    void tentativaDuranteSuspensaoNaoConfereSenha() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        falharSenha(4);

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("joao@umc.br", "Senha@123")))
                .isInstanceOf(LimiteDeTentativasException.class)
                .hasMessage(MENSAGEM_LOGIN_INVALIDO);

        verify(passwordEncoder, times(4)).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("quatro falhas depois da pausa suspendem por dois minutos")
    void segundaPausaDuraDoisMinutos() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        falharSenha(4);
        relogio.avancar(Duration.ofMinutes(2));
        falharSenha(4);

        assertThat(usuario.getSenhaPausaOcorrencias()).isEqualTo(2);
        assertThat(usuario.getSenhaBloqueadoAte()).isEqualTo(INICIO.plusMinutes(4));
    }

    @Test
    @DisplayName("login correto zera os cinco contadores de senha")
    void loginCorretoZeraContadores() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);
        when(passwordEncoder.matches("Senha@123", "$2a$10$hash")).thenReturn(true);
        when(codigoVerificacaoService.gerarCodigo(usuario, TipoCodigoEnum.MFA)).thenReturn("1234");

        falharSenha(3);
        authService.login(new LoginRequestDTO("joao@umc.br", "Senha@123"));

        assertThat(usuario.getSenhaErrosConsecutivos()).isZero();
        assertThat(usuario.getSenhaFalhasJanela()).isZero();
        assertThat(usuario.getSenhaJanelaInicio()).isNull();
        assertThat(usuario.getSenhaPausaOcorrencias()).isNull();
        assertThat(usuario.getSenhaBloqueadoAte()).isNull();
    }

    @Test
    @DisplayName("as ocorrencias caducam em 24 horas e a pausa volta a um minuto")
    void ocorrenciasCaducamEmVinteQuatroHoras() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        falharSenha(4);
        assertThat(usuario.getSenhaPausaOcorrencias()).isEqualTo(1);

        relogio.avancar(Duration.ofHours(24).plusMinutes(2));
        falharSenha(4);

        assertThat(usuario.getSenhaPausaOcorrencias()).isEqualTo(1);
        assertThat(usuario.getSenhaBloqueadoAte())
                .isEqualTo(INICIO.plusHours(24).plusMinutes(3));
    }

    @Test
    @DisplayName("o orcamento de 45 falhas por hora dispara o bloqueio de uma hora")
    void orcamentoDeQuarentaECincoPorHora() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hash")).thenReturn(false);

        boolean bloqueioDeUmaHora = false;
        int rodadas = 0;

        while (!bloqueioDeUmaHora && rodadas < 200) {
            rodadas++;
            LocalDateTime antes = LocalDateTime.now(relogio);
            try {
                authService.login(new LoginRequestDTO("joao@umc.br", "errada"));
            } catch (CredenciaisInvalidasException falhaComum) {
                // falha contada no orcamento
            } catch (LimiteDeTentativasException bloqueado) {
                Duration espera = Duration.between(antes, usuario.getSenhaBloqueadoAte());
                if (espera.toMinutes() >= 60) {
                    bloqueioDeUmaHora = true;
                } else {
                    relogio.avancar(espera.plusSeconds(1));
                }
            }
        }

        assertThat(bloqueioDeUmaHora).isTrue();
        assertThat(LocalDateTime.now(relogio))
                .isAfter(INICIO.plusMinutes(44))
                .isBefore(INICIO.plusHours(1));

        verify(auditoriaService, times(45)).registrar(
                eq(AcaoAuditoriaEnum.LOGIN_FALHOU), eq(RecursoAuditoriaEnum.USUARIO), any(), any());
        verify(auditoriaService).registrar(
                eq(AcaoAuditoriaEnum.LOGIN_BLOQUEADO), eq(RecursoAuditoriaEnum.USUARIO), any(), any());
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

    @Test
    @DisplayName("no login, o bloqueio por limite chega ao chamador como 429")
    void bloqueioNoLoginNaoEhEngolido() {
        var usuario = umUsuarioAtivo();
        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", "$2a$10$hash")).thenReturn(true);
        when(codigoVerificacaoService.gerarCodigo(usuario, TipoCodigoEnum.MFA))
                .thenThrow(new LimiteDeTentativasException("Muitos pedidos"));

        assertThatThrownBy(() -> authService.login(new LoginRequestDTO("joao@umc.br", "Senha@123")))
                .isInstanceOf(LimiteDeTentativasException.class);

        verify(emailService, never()).enviar(any(), any(), any());
    }


    @Test
    @DisplayName("verificar codigo com e-mail inexistente devolve a mesma mensagem de codigo invalido")
    void verificarCodigoComEmailInexistente() {
        when(usuarioRepository.findByEmail("ninguem@umc.br")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verificarCodigo(
                new VerificarCodigoRequestDTO("ninguem@umc.br", "1234")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Codigo invalido ou expirado");

        verify(codigoVerificacaoService, never()).validarCodigo(any(), any(), any());
    }

    private static final class RelogioAjustavel extends Clock {
        private Instant instante;

        RelogioAjustavel(LocalDateTime inicio) {
            this.instante = inicio.atZone(ZoneId.systemDefault()).toInstant();
        }

        void avancar(Duration duracao) {
            this.instante = this.instante.plus(duracao);
        }

        @Override public ZoneId getZone() { return ZoneId.systemDefault(); }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return instante; }
    }

    @Test
    @DisplayName("professor com CREF pendente recebe papel ALUNO na resposta e na claim")
    void professorPendenteRecebePapelDeAluno() {
        var usuario = umProfessor(StatusAprovacaoEnum.PENDENTE);

        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));

        var resposta = authService.verificarCodigo(
                new VerificarCodigoRequestDTO("joao@umc.br", "1234"));

        assertThat(resposta.papel()).isEqualTo(PapelUsuarioEnum.ALUNO);
        assertThat(claimDePapel(resposta.token())).isEqualTo("ALUNO");
    }

    @Test
    @DisplayName("professor com CREF aprovado recebe papel PROFESSOR")
    void professorAprovadoRecebePapelDeProfessor() {
        var usuario = umProfessor(StatusAprovacaoEnum.APROVADO);

        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));

        var resposta = authService.verificarCodigo(
                new VerificarCodigoRequestDTO("joao@umc.br", "1234"));

        assertThat(resposta.papel()).isEqualTo(PapelUsuarioEnum.PROFESSOR);
        assertThat(claimDePapel(resposta.token())).isEqualTo("PROFESSOR");
    }

    @Test
    @DisplayName("professor com CREF rejeitado continua com papel ALUNO")
    void professorRejeitadoRecebePapelDeAluno() {
        var usuario = umProfessor(StatusAprovacaoEnum.REJEITADO);

        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));

        var resposta = authService.verificarCodigo(
                new VerificarCodigoRequestDTO("joao@umc.br", "1234"));

        assertThat(resposta.papel()).isEqualTo(PapelUsuarioEnum.ALUNO);
        assertThat(claimDePapel(resposta.token())).isEqualTo("ALUNO");
    }

    @Test
    @DisplayName("professor sem situacao de CREF recebe papel ALUNO, e nao permissao por omissao")
    void professorSemSituacaoDeCrefRecebePapelDeAluno() {
        var usuario = umProfessor(null);

        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));

        var resposta = authService.verificarCodigo(
                new VerificarCodigoRequestDTO("joao@umc.br", "1234"));

        assertThat(resposta.papel()).isEqualTo(PapelUsuarioEnum.ALUNO);
        assertThat(claimDePapel(resposta.token())).isEqualTo("ALUNO");
    }

    @Test
    @DisplayName("administrador nao e afetado pela verificacao do CREF")
    void administradorNaoEAfetadoPelaVerificacaoDoCref() {
        var usuario = umUsuarioAtivo();
        usuario.setPapel(PapelUsuarioEnum.ADMINISTRADOR);

        when(usuarioRepository.findByEmail("joao@umc.br")).thenReturn(Optional.of(usuario));

        var resposta = authService.verificarCodigo(
                new VerificarCodigoRequestDTO("joao@umc.br", "1234"));

        assertThat(resposta.papel()).isEqualTo(PapelUsuarioEnum.ADMINISTRADOR);
        assertThat(claimDePapel(resposta.token())).isEqualTo("ADMINISTRADOR");
    }

    private UsuarioEntity umProfessor(StatusAprovacaoEnum situacaoCref) {
        var entity = umUsuarioAtivo();
        entity.setPapel(PapelUsuarioEnum.PROFESSOR);
        entity.setCref("123456-G/SP");
        entity.setSituacaoCref(situacaoCref);
        return entity;
    }

    private String claimDePapel(String token) {
        return segurancaConfig.jwtDecoder(SEGREDO).decode(token).getClaimAsString("papel");
    }

    private UsuarioEntity umUsuarioAtivo() {
        var entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNomeCompleto("Joao da Silva");
        entity.setDataNascimento(LocalDate.of(2000, 1, 1));
        entity.setEmail("joao@umc.br");
        entity.setSenhaHash("$2a$10$hash");
        entity.setPapel(PapelUsuarioEnum.ALUNO);
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.now());
        return entity;
    }

    private void falharSenha(int vezes) {
        for (int i = 0; i < vezes; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequestDTO("joao@umc.br", "errada")))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }
    }
}