package forcamente.api.cifra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CifraDeEmail - o e-mail vai cifrado para o banco e volta igual, e o indice acha a conta sem decifrar")
class CifraDeEmailTest {

    private static final String EMAIL = "maria.silva@umc.br";

    private final CifraDeEmail cifra = new CifraDeEmail(chave(1), chave(2));

    @Test
    @DisplayName("o texto cifrado comeca com v1:, nao contem o endereco e volta ao e-mail original")
    void deveCifrarEDecifrar() {
        String cifrado = cifra.cifrar(EMAIL);

        assertThat(cifrado).startsWith(CifraDeEmail.PREFIXO).doesNotContain("maria").doesNotContain("umc.br");
        assertThat(cifra.decifrar(cifrado)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("o mesmo e-mail cifrado duas vezes gera textos diferentes, para o banco nao revelar contas com o mesmo e-mail")
    void deveGerarTextoDiferenteACadaVez() {
        assertThat(cifra.cifrar(EMAIL)).isNotEqualTo(cifra.cifrar(EMAIL));
    }

    @Test
    @DisplayName("valor sem o prefixo v1: e e-mail ainda em claro: volta como esta, ate o back cifrar ao subir")
    void deveDevolverEmailEmClaroSemMudar() {
        assertThat(cifra.decifrar(EMAIL)).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("e-mail nulo continua nulo na cifra, na decifra e no indice, porque o e-mail pendente costuma ser nulo")
    void deveTratarNulo() {
        assertThat(cifra.cifrar(null)).isNull();
        assertThat(cifra.decifrar(null)).isNull();
        assertThat(cifra.indice(null)).isNull();
    }

    @Test
    @DisplayName("o indice ignora maiusculas e espacos nas pontas, entao o login acha a conta de qualquer jeito que o e-mail for digitado")
    void deveCalcularOMesmoIndiceComMaiusculas() {
        assertThat(cifra.indice("  Maria.Silva@UMC.br ")).isEqualTo(cifra.indice(EMAIL));
        assertThat(cifra.indice(EMAIL)).hasSize(64).matches("[0-9a-f]+");
        assertThat(cifra.indice("outra.pessoa@umc.br")).isNotEqualTo(cifra.indice(EMAIL));
    }

    @Test
    @DisplayName("o indice depende da chave: sem ela, nao da para calcular o indice de uma lista de e-mails e comparar")
    void deveMudarOIndiceComOutraChave() {
        CifraDeEmail outra = new CifraDeEmail(chave(1), chave(3));

        assertThat(outra.indice(EMAIL)).isNotEqualTo(cifra.indice(EMAIL));
    }

    @Test
    @DisplayName("texto cifrado com outra chave e recusado, em vez de virar um e-mail errado")
    void deveRecusarChaveErrada() {
        String cifradoPorOutra = new CifraDeEmail(chave(4), chave(2)).cifrar(EMAIL);

        assertThatThrownBy(() -> cifra.decifrar(cifradoPorOutra)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("texto cifrado adulterado e recusado, porque o AES-GCM confere a integridade")
    void deveRecusarTextoAdulterado() {
        String cifrado = cifra.cifrar(EMAIL);
        byte[] bytes = Base64.getDecoder().decode(cifrado.substring(CifraDeEmail.PREFIXO.length()));
        bytes[bytes.length - 1] ^= 1;
        String adulterado = CifraDeEmail.PREFIXO + Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> cifra.decifrar(adulterado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("chave fora do formato impede o back de subir: precisa ser Base64 de exatamente 32 bytes")
    void deveRecusarChaveForaDoFormato() {
        String curta = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new CifraDeEmail(curta, chave(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> new CifraDeEmail("isto nao e base64", chave(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    private static String chave(int preenchimento) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) preenchimento);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
