package forcamente.api.exception;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;


@Component
public class TratadorDeErrosDeSeguranca  implements AuthenticationEntryPoint, AccessDeniedHandler{

    private final HandlerExceptionResolver resolver;

    public TratadorDeErrosDeSeguranca(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException excecao) {
        resolver.resolveException(request, response, null, excecao);
    }

    @Override
    public void  handle(HttpServletRequest request, HttpServletResponse response,AccessDeniedException exececao){
        resolver.resolveException(request, response, null, exececao);
    }
}
