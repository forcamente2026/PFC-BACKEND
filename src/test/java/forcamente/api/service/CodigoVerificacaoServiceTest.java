package forcamente.api.service;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.TipoCodigoEnum;
import forcamente.api.exception.CredenciaisInvalidasException;
import forcamente.api.exception.LimiteDeTentativasException;
import forcamente.api.service.impl.CodigoVerificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CodigoVerificacaoService - geracao, validade, tentativas e bloqueio")
class CodigoVerificacaoServiceTest {

    private RelogioDeTeste relogio;

    private CodigoVerificacaoService servico;

    private UsuarioEntity usuario;

    @BeforeEach
    void configurar() {
        relogio = new RelogioDeTeste(Instant.parse("2026-09-25T10:00:00Z"));
        servico = new CodigoVerificacaoService(new BCryptPasswordEncoder(), relogio);
        usuario = new UsuarioEntity();
        usuario.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("deve gerar codigo de 4 digitos, guardar hash e nao o texto puro")
    void deveGerarCodigoComHash() {
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);

        assertThat(codigo).hasSize(4).containsOnlyDigits();
        assertThat(usuario.getCodigoHash()).isNotEqualTo(codigo).startsWith("$2");
        assertThat(usuario.getCodigoTipo()).isEqualTo(TipoCodigoEnum.MFA);
        assertThat(usuario.getCodigoTentativas()).isZero();
    }

    @Test
    @DisplayName("deve validar o codigo correto e anular o codigo depois do uso")
    void deveValidarEAnular() {
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);

        servico.validarCodigo(usuario, TipoCodigoEnum.MFA, codigo);

        assertThat(usuario.getCodigoHash()).isNull();
    }

    @Test
    @DisplayName("nao deve aceitar o mesmo codigo duas vezes")
    void naoDeveAceitarCodigoUsado() {
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);
        servico.validarCodigo(usuario, TipoCodigoEnum.MFA, codigo);

        assertThatThrownBy(() -> servico.validarCodigo(usuario, TipoCodigoEnum.MFA, codigo))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("codigo de MFA expira em 2 minutos")
    void codigoMfaExpiraEmDoisMinutos() {
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);

        relogio.avancar(Duration.ofMinutes(2).plusSeconds(1));

        assertThatThrownBy(() -> servico.validarCodigo(usuario, TipoCodigoEnum.MFA, codigo))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessageContaining("invalido ou expirado");
    }

    @Test
    @DisplayName("codigo de redefinicao dura 15 minutos")
    void codigoRedefinicaoDuraQuinzeMinutos() {
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA);

        relogio.avancar(Duration.ofMinutes(14));

        servico.validarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA, codigo);

        assertThat(usuario.getCodigoHash()).isNull();
    }

    @Test
    @DisplayName("duas digitacoes erradas anulam o codigo")
    void duasErradasAnulamOCodigo() {
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);
        String errado = codigo.equals("0000") ? "1111" : "0000";

        assertThatThrownBy(() -> servico.validarCodigo(usuario, TipoCodigoEnum.MFA, errado))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(usuario.getCodigoHash()).isNotNull();

        assertThatThrownBy(() -> servico.validarCodigo(usuario, TipoCodigoEnum.MFA, errado))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(usuario.getCodigoHash()).isNull();
    }

    @Test
    @DisplayName("nao deve aceitar codigo de outro tipo")
    void naoDeveAceitarCodigoDeOutroTipo() {
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA);

        assertThatThrownBy(() -> servico.validarCodigo(usuario, TipoCodigoEnum.MFA, codigo))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("setimo pedido de MFA na mesma hora bloqueia por 1 hora")
    void setimoPedidoMfaBloqueia() {
        for (int i = 0; i < 6; i++) {
            servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);
        }

        assertThatThrownBy(() -> servico.gerarCodigo(usuario, TipoCodigoEnum.MFA))
                .isInstanceOf(LimiteDeTentativasException.class)
                .hasMessageContaining("1 hora");

        assertThat(usuario.getMfaBloqueadoAte()).isNotNull();
    }

    @Test
    @DisplayName("passada a hora, a janela reabre com 6 pedidos novos")
    void janelaDeMfaReabreDepoisDeUmaHora() {
        for (int i = 0; i < 6; i++) {
            servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);
        }
        assertThatThrownBy(() -> servico.gerarCodigo(usuario, TipoCodigoEnum.MFA))
                .isInstanceOf(LimiteDeTentativasException.class);

        relogio.avancar(Duration.ofHours(1).plusMinutes(1));

        assertThat(servico.gerarCodigo(usuario, TipoCodigoEnum.MFA)).hasSize(4);
    }

    @Test
    @DisplayName("segundo bloqueio de MFA dura 2 horas")
    void segundoBloqueioDuraDuasHoras() {
        estourarPedidosDeMfa();
        relogio.avancar(Duration.ofHours(1).plusMinutes(1));
        estourarPedidosDeMfa();

        assertThat(usuario.getMfaOcorrenciasBloqueio()).isEqualTo(2);
        assertThat(usuario.getMfaBloqueadoAte())
                .isEqualTo(java.time.LocalDateTime.now(relogio).plusHours(2));
    }

    @Test
    @DisplayName("setimo pedido de redefinicao no mesmo dia e recusado")
    void setimoPedidoDeRedefinicaoNoMesmoDia() {
        for (int i = 0; i < 6; i++) {
            servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA);
        }

        assertThatThrownBy(() -> servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA))
                .isInstanceOf(LimiteDeTentativasException.class)
                .hasMessageContaining("diario");
    }

    @Test
    @DisplayName("no dia seguinte a cota de redefinicao volta")
    void cotaDeRedefinicaoVoltaNoDiaSeguinte() {
        for (int i = 0; i < 6; i++) {
            servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA);
        }

        relogio.avancar(Duration.ofDays(1));

        assertThat(servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA)).hasSize(4);
    }

    @Test
    @DisplayName("seis erros consecutivos na redefinicao bloqueiam por 4 horas")
    void seisErrosConsecutivosBloqueiamPorQuatroHoras() {
        for (int i = 0; i < 3; i++) {
            String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA);
            String errado = codigo.equals("0000") ? "1111" : "0000";

            assertThatThrownBy(() -> servico.validarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA, errado))
                    .isInstanceOf(CredenciaisInvalidasException.class);
            assertThatThrownBy(() -> servico.validarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA, errado))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }

        assertThat(usuario.getRedefinicaoBloqueadoAte()).isNotNull();

        assertThatThrownBy(() -> servico.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA))
                .isInstanceOf(LimiteDeTentativasException.class);
    }

    @Test
    @DisplayName("sucesso zera os contadores do fluxo")
    void sucessoZeraContadores() {
        servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);
        String codigo = servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);

        servico.validarCodigo(usuario, TipoCodigoEnum.MFA, codigo);

        assertThat(usuario.getMfaPedidos()).isZero();
        assertThat(usuario.getMfaJanelaInicio()).isNull();
    }

    private void estourarPedidosDeMfa() {
        for (int i = 0; i < 6; i++) {
            servico.gerarCodigo(usuario, TipoCodigoEnum.MFA);
        }
        assertThatThrownBy(() -> servico.gerarCodigo(usuario, TipoCodigoEnum.MFA))
                .isInstanceOf(LimiteDeTentativasException.class);
    }

    private static class RelogioDeTeste extends Clock {

        private Instant instante;

        private RelogioDeTeste(Instant instante) {
            this.instante = instante;
        }

        private void avancar(Duration duracao) {
            instante = instante.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}