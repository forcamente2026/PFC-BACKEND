package forcamente.api.config;

import forcamente.api.exception.LimiteDeTentativasException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LimitePorIpFilter extends OncePerRequestFilter {

    private static final int CHAVES_PARA_LIMPAR = 10_000;

    private static final String MENSAGEM =
            "Muitas requisicoes deste endereco. Tente novamente mais tarde.";

    private final HandlerExceptionResolver resolver;

    private final Clock clock;

    private final int requisicoes;

    private final int janelaMinutos;

    private final Map<String, Janela> janelasPorChave = new ConcurrentHashMap<>();

    public LimitePorIpFilter(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver,
                             Clock clock,
                             @Value("${limite-por-ip.requisicoes}") int requisicoes,
                             @Value("${limite-por-ip.janela-minutos}") int janelaMinutos) {
        this.resolver = resolver;
        this.clock = clock;
        this.requisicoes = requisicoes;
        this.janelaMinutos = janelaMinutos;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return grupoDe(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        LocalDateTime agora = LocalDateTime.now(clock);

        if (excedeu(request.getRemoteAddr() + "|" + grupoDe(request), agora)) {
            log.warn("Limite por IP atingido: caminho={}", request.getRequestURI());

            resolver.resolveException(request, response, null,
                    new LimiteDeTentativasException(MENSAGEM));
            return;
        }

        chain.doFilter(request, response);
    }

    private String grupoDe(HttpServletRequest request) {
        String caminho = request.getRequestURI();

        if (caminho.startsWith("/api/auth/")) {
            return "auth";
        }

        if (caminho.startsWith("/api/enderecos/")) {
            return "enderecos";
        }

        if ("POST".equals(request.getMethod()) && caminho.equals("/api/usuarios")) {
            return "cadastro";
        }

        return null;
    }

    private boolean excedeu(String chave, LocalDateTime agora) {
        limparSeCrescerDemais(agora);

        Janela janela = janelasPorChave.compute(chave, (ignorada, atual) ->
                atual == null || atual.inicio().plusMinutes(janelaMinutos).isBefore(agora)
                        ? new Janela(agora, 1)
                        : new Janela(atual.inicio(), atual.contagem() + 1));

        return janela.contagem() > requisicoes;
    }

    private void limparSeCrescerDemais(LocalDateTime agora) {
        if (janelasPorChave.size() < CHAVES_PARA_LIMPAR) {
            return;
        }

        janelasPorChave.values().removeIf(
                janela -> janela.inicio().plusMinutes(janelaMinutos).isBefore(agora));
    }

    private record Janela(LocalDateTime inicio, int contagem) {
    }
}
