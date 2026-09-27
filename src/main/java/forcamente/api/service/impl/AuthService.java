package forcamente.api.service.impl;


import forcamente.api.dto.*;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.TipoCodigoEnum;
import forcamente.api.exception.CredenciaisInvalidasException;
import forcamente.api.exception.LimiteDeTentativasException;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IAuditoriaService;
import forcamente.api.service.IAuthService;
import forcamente.api.service.ICodigoVerificacaoService;
import forcamente.api.service.IEmailService;
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
import java.util.UUID;

@Service
@Slf4j
public class AuthService  implements IAuthService {
    private  final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long expiracaoMinutos;
    private final ICodigoVerificacaoService codigoVerificacaoService;
    private final IEmailService emailService;
    private final IAuditoriaService auditoriaService;
    private static final String MENSAGEM_CODIGO_INVALIDO = "Codigo invalido ou expirado";
    private static final String ASSUNTO_MFA = "ForcaMente - seu codigo de acesso";
    private String textoMfa(String codigo) {
        return """
                Alguem esta entrando na sua conta do ForcaMente.

                Seu codigo de acesso e: %s

                Ele vale por 2 minutos e pode ser usado uma unica vez.

                Se nao foi voce, ignore esta mensagem e troque a sua senha.
                """.formatted(codigo);
    }

    public AuthService(IUsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder,
                       ICodigoVerificacaoService codigoVerificacaoService,
                       IEmailService emailService,
                       IAuditoriaService auditoriaService,
                       @Value("${jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.codigoVerificacaoService = codigoVerificacaoService;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    @Override
    @Transactional(noRollbackFor = LimiteDeTentativasException.class)
    public LoginPendenteResponseDTO login(LoginRequestDTO loginRequestDTO) {
        log.info("login");

        UsuarioEntity usuario = usuarioRepository.findByEmail(loginRequestDTO.email()).orElse(null);

        if (usuario == null
                || !usuario.isAtivo()
                || !passwordEncoder.matches(loginRequestDTO.senha(), usuario.getSenhaHash())) {

            UUID idTentado = usuario == null ? null : usuario.getId();
            auditoriaService.registrar(
                    AcaoAuditoriaEnum.LOGIN_FALHOU, RecursoAuditoriaEnum.USUARIO, idTentado, idTentado);

            throw new CredenciaisInvalidasException("E-mail ou senha invalidos");
        }
        String codigo = codigoVerificacaoService.gerarCodigo(usuario, TipoCodigoEnum.MFA);
                usuarioRepository.save(usuario);
                emailService.enviar(usuario.getEmail(), ASSUNTO_MFA, textoMfa(codigo));
                return new LoginPendenteResponseDTO(
                        true, codigoVerificacaoService.validadeEmSegundos(TipoCodigoEnum.MFA)
                );


    }

    private String gerarToken(UsuarioEntity usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("forcamente-api").subject(usuario.getId().toString()).claim("papel",usuario.getPapel().name()).issuedAt(agora).expiresAt(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)).build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }

    private static final String ASSUNTO_REDEFINICAO = "ForcaMente - codigo para redefinir sua senha";

    private String textoRedefinicao(String codigo) {
        return """
                Recebemos um pedido para redefinir a sua senha no ForcaMente.

                Seu codigo e: %s

                Ele vale por 15 minutos e pode ser usado uma unica vez.

                Se voce nao solicitou esta troca, ignore esta mensagem: sua senha continua a mesma.
                """.formatted(codigo);

    }

    @Override
    @Transactional
    public void esqueciSenha(EsqueciSenhaRequestDTO esqueciSenhaRequestDTO) {
        log.info("Pedido de redefinicao de senha recebido");

        UsuarioEntity usuario = usuarioRepository.findByEmail(esqueciSenhaRequestDTO.email()).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return;
        }
        String codigo;
        try {
            codigo = codigoVerificacaoService.gerarCodigo(usuario, TipoCodigoEnum.REDEFINICAO_SENHA);
        } catch (LimiteDeTentativasException excecao) {
            log.info("Pedido de redefinicao recusado por limite: usuario={}", usuario.getId());
            return;
        }
        usuarioRepository.save(usuario);

        emailService.enviarAssincrono(usuario.getEmail(), ASSUNTO_REDEFINICAO, textoRedefinicao(codigo));
    }

    @Override
    @Transactional(noRollbackFor = {CredenciaisInvalidasException.class, LimiteDeTentativasException.class})
    public void redefinirSenha(RedefinirSenhaRequestDTO redefinirSenhaRequestDTO) {
        log.info("Redefinicao de senha recebida");

        UsuarioEntity usuario = usuarioRepository.findByEmail(redefinirSenhaRequestDTO.email()).orElseThrow(()-> new CredenciaisInvalidasException("Codigo invalido ou expirado"));

        if (!usuario.isAtivo()){
            throw new CredenciaisInvalidasException("Codigo invalido ou expírado");
        }
        codigoVerificacaoService.validarCodigo(
                usuario, TipoCodigoEnum.REDEFINICAO_SENHA, redefinirSenhaRequestDTO.codigo()
        );
        usuario.setSenhaHash(passwordEncoder.encode(redefinirSenhaRequestDTO.novaSenha()));
        usuarioRepository.save(usuario);

    log.info("Senha redefinida: usuario={}", usuario.getId());
    }

    @Override
    @Transactional(noRollbackFor = {CredenciaisInvalidasException.class, LimiteDeTentativasException.class})
    public LoginResponseDTO verificarCodigo(VerificarCodigoRequestDTO verificarCodigoRequestDTO) {
        log.info("Verificacao de codigo de login recebida");

        UsuarioEntity usuario = usuarioRepository.findByEmail(verificarCodigoRequestDTO.email()).orElseThrow(() -> new CredenciaisInvalidasException(MENSAGEM_CODIGO_INVALIDO));

        if (!usuario.isAtivo()) {
            throw new CredenciaisInvalidasException(MENSAGEM_CODIGO_INVALIDO);
        }
        try {
            codigoVerificacaoService.validarCodigo(
                    usuario, TipoCodigoEnum.MFA, verificarCodigoRequestDTO.codigo()
            );
        } catch (CredenciaisInvalidasException excecao) {
            auditoriaService.registrar(
                    AcaoAuditoriaEnum.MFA_FALHOU, RecursoAuditoriaEnum.USUARIO,
                    usuario.getId(), usuario.getId());
            throw excecao;
        }

        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.LOGIN_REALIZADO, RecursoAuditoriaEnum.USUARIO,
                usuario.getId(), usuario.getId());

        return new LoginResponseDTO(gerarToken(usuario), usuario.getNomeCompleto(),usuario.getPapel());

    }
}
