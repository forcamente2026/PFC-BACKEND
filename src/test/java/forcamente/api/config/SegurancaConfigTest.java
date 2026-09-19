package forcamente.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SegurancaConfig - geracao e validacao de JWT")
class SegurancaConfigTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-trinta-e-dois-caracteres";
    private static final String OUTRO_SEGREDO = "outro-segredo-diferente-tambem-com-mais-de-trinta-e-dois";

    private final SegurancaConfig config = new SegurancaConfig();

    @Test
    @DisplayName("token assinado com o segredo deve ser aceito e devolver as claims")
    void deveGerarEValidarToken() {
        var token = gerarToken(SEGREDO);

        var jwt = config.jwtDecoder(SEGREDO).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("123");
        assertThat(jwt.getClaimAsString("papel")).isEqualTo("ALUNO");
    }

    @Test
    @DisplayName("token assinado com outro segredo deve ser recusado")
    void deveRecusarTokenComOutroSegredo() {
        var token = gerarToken(OUTRO_SEGREDO);

        assertThatThrownBy(() -> config.jwtDecoder(SEGREDO).decode(token))
                .isInstanceOf(JwtException.class);
    }

    private String gerarToken(String segredo) {
        var claims = JwtClaimsSet.builder()
                .subject("123")
                .claim("papel", "ALUNO")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        var cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        return config.jwtEncoder(segredo)
                .encode(JwtEncoderParameters.from(cabecalho, claims))
                .getTokenValue();
    }
}