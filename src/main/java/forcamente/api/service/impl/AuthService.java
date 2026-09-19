package forcamente.api.service.impl;


import forcamente.api.dto.LoginRequestDTO;
import forcamente.api.dto.LoginResponseDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.exception.CredenciaisInvalidasException;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class AuthService  implements IAuthService {
    private  final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long expiracaoMinutos;

    public AuthService(IUsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       @Value("${jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    @Override
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
        log.info("login");

        UsuarioEntity usuario = usuarioRepository.findByEmail(loginRequestDTO.email()).orElse(null);

        if (usuario == null
                || !usuario.isAtivo()
                || !passwordEncoder.matches(loginRequestDTO.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("E-mail ou senha invalidos");
        }

        return new LoginResponseDTO(gerarToken(usuario), usuario.getNomeCompleto());
    }

    private String gerarToken(UsuarioEntity usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("forcamente-api").subject(usuario.getId().toString()).claim("papel",usuario.getPapel().name()).issuedAt(agora).expiresAt(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)).build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }
}
