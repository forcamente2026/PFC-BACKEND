package forcamente.api.config;


import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.exception.TratadorDeErrosDeSeguranca;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;


@Configuration
@EnableWebSecurity
public class SegurancaConfig {
    private static final String[] EDITORES = {
            PapelUsuarioEnum.PROFESSOR.name(),
            PapelUsuarioEnum.ADMINISTRADOR.name()
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var conversorDePapel = new JwtGrantedAuthoritiesConverter();
        conversorDePapel.setAuthoritiesClaimName("papel");
        conversorDePapel.setAuthorityPrefix("ROLE_");

        var conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(conversorDePapel);
        return conversor;
    }

    @Bean
    public SecurityFilterChain cadeiaDeFiltros(HttpSecurity http,
                                               TratadorDeErrosDeSeguranca tratador,
                                               JwtAuthenticationConverter conversor) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/usuarios").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/exercicios/**").hasAnyRole(EDITORES)
                        .requestMatchers(HttpMethod.PUT, "/api/exercicios/**").hasAnyRole(EDITORES)
                        .requestMatchers(HttpMethod.DELETE, "/api/exercicios/**").hasAnyRole(EDITORES)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(tratador)
                        .accessDeniedHandler(tratador))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(tratador)
                        .accessDeniedHandler(tratador));

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
