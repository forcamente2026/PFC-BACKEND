package forcamente.api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mascaras - o formato que o front vai receber, campo por campo")
class MascarasTest {

    @Test
    @DisplayName("o e-mail mantem a primeira letra e o dominio inteiro")
    void deveMascararOEmailMantendoODominio() {
        assertThat(Mascaras.email("joao.silva@gmail.com")).isEqualTo("j***@gmail.com");
        assertThat(Mascaras.email("ana.ramos@exemplo.test")).isEqualTo("a***@exemplo.test");
    }

    @Test
    @DisplayName("e-mail sem arroba ou comecando por arroba sai oculto por inteiro")
    void deveOcultarOEmailMalFormado() {
        assertThat(Mascaras.email("semarroba")).isEqualTo("***");
        assertThat(Mascaras.email("@dominio.com")).isEqualTo("***");
    }

    @Test
    @DisplayName("a data de nascimento mantem so o ano")
    void deveMascararADataMantendoOAno() {
        assertThat(Mascaras.dataNascimento("15/03/1998")).isEqualTo("**/**/1998");
        assertThat(Mascaras.dataNascimento("01/01/2000")).isEqualTo("**/**/2000");
    }

    @Test
    @DisplayName("o CREF mantem o primeiro digito, os dois ultimos, a letra e a UF")
    void deveMascararOCrefMantendoAUf() {
        assertThat(Mascaras.cref("123456-G/SP")).isEqualTo("1***56-G/SP");
        assertThat(Mascaras.cref("987654-L/RJ")).isEqualTo("9***54-L/RJ");
    }

    @Test
    @DisplayName("nulo e vazio atravessam sem virar a palavra nula")
    void deveDeixarNuloComoNulo() {
        assertThat(Mascaras.email(null)).isNull();
        assertThat(Mascaras.dataNascimento(null)).isNull();
        assertThat(Mascaras.cref(null)).isNull();
        assertThat(Mascaras.cref("")).isEmpty();
        assertThat(Mascaras.mascarar((UsuarioDetalheResponseDTO) null)).isNull();
    }

    @Test
    @DisplayName("a mascara nao devolve o valor original em nenhum dos tres campos")
    void naoDeveDeixarPassarOValorOriginal() {
        assertThat(Mascaras.email("joao.silva@gmail.com")).doesNotContain("oao.silva");
        assertThat(Mascaras.dataNascimento("15/03/1998")).doesNotContain("15").doesNotContain("03");
        assertThat(Mascaras.cref("123456-G/SP")).doesNotContain("2345");
    }
}
