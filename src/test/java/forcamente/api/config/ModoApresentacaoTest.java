package forcamente.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModoApresentacao - nasce do ambiente e troca em tempo de execucao")
class ModoApresentacaoTest {

    @Test
    @DisplayName("sem a variavel, nasce desligado: o padrao de fabrica nao enfraquece o segundo fator")
    void deveNascerDesligadoPorPadrao() {
        assertThat(new ModoApresentacao(false).isAtivo()).isFalse();
    }

    @Test
    @DisplayName("com a variavel ligada, nasce ligado, para a apresentacao nao depender do e-mail")
    void deveNascerLigadoPelaVariavel() {
        var modo = new ModoApresentacao(true);

        assertThat(modo.isAtivo()).isTrue();
        assertThat(modo.getCodigoFixo()).isEqualTo("0000");
    }

    @Test
    @DisplayName("quem nasceu ligado pode ser desligado sem reiniciar, e religado")
    void deveTrocarEmTempoDeExecucao() {
        var modo = new ModoApresentacao(true);

        modo.definir(false);
        assertThat(modo.isAtivo()).isFalse();

        modo.definir(true);
        assertThat(modo.isAtivo()).isTrue();
    }
}
