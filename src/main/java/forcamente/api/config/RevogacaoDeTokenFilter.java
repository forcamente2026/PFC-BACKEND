package forcamente.api.config;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.exception.CredenciaisInvalidasException;
import forcamente.api.repository.IUsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class RevogacaoDeTokenFilter extends OncePerRequestFilter {

    private static final String MENSAGEM = "Sessao encerrada. Entre novamente.";

    private final IUsuarioRepository usuarioRepository;

    private final HandlerExceptionResolver resolver;

    private final Clock clock;

    public RevogacaoDeTokenFilter(IUsuarioRepository usuarioRepository,
                                  @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
                                  Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.resolver = resolver;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Jwt token = tokenAutenticado();

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        UsuarioEntity usuario = usuarioDe(token);

        if (usuario != null && (!usuario.isAtivo() || revogado(usuario, token))) {
            log.info("Token recusado por revogacao: caminho={}", request.getRequestURI());

            SecurityContextHolder.clearContext();
            resolver.resolveException(request, response, null,
                    new CredenciaisInvalidasException(MENSAGEM));
            return;
        }

        chain.doFilter(request, response);
    }

    private Jwt tokenAutenticado() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();

        return autenticacao instanceof JwtAuthenticationToken jwt ? jwt.getToken() : null;
    }

    private UsuarioEntity usuarioDe(Jwt token) {
        try {
            return usuarioRepository.findById(UUID.fromString(token.getSubject())).orElse(null);
        } catch (IllegalArgumentException | NullPointerException excecao) {
            return null;
        }
    }

    private boolean revogado(UsuarioEntity usuario, Jwt token) {
        if (usuario.getTokenValidoApos() == null) {
            return false;
        }

        Instant emitidoEm = token.getIssuedAt();

        if (emitidoEm == null) {
            return true;
        }

        LocalDateTime emitido = LocalDateTime.ofInstant(emitidoEm, clock.getZone());

        return !emitido.isAfter(usuario.getTokenValidoApos());
    }
}
