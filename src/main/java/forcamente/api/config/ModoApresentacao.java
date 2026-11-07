package forcamente.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ModoApresentacao {

    private static final String CODIGO_FIXO = "0000";

    private volatile boolean ativo = false;

    public boolean isAtivo() {
        return ativo;
    }

    public String getCodigoFixo() {
        return CODIGO_FIXO;
    }

    public void definir(boolean ativo) {
        this.ativo = ativo;

        if (ativo) {
            log.warn("MODO APRESENTACAO LIGADO: o codigo de verificacao passou a ser fixo. "
                    + "Isso enfraquece o segundo fator e vale para login, redefinicao de senha "
                    + "e troca de e-mail. Reiniciar a aplicacao desliga.");
        } else {
            log.warn("MODO APRESENTACAO DESLIGADO: o codigo de verificacao voltou a ser sorteado.");
        }
    }
}
