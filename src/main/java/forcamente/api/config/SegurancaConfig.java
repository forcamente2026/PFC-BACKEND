package forcamente.api.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;


@Configuration
@EnableWebSecurity
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadeiaDeFiltros(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers("/api/auth/**", "/api/usuarios/**", "/api/exercicios/**", "/api/metricas/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public JwtEncoder jwtEncoder (@Value("${jwt.segredo}") String segredo) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveHmac(segredo)));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.segredo}") String segredo) {
        return NimbusJwtDecoder.withSecretKey(chaveHmac(segredo)).build();
    }
    private SecretKey chaveHmac(String segredo) {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
