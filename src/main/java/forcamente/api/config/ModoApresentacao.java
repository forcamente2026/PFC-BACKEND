package forcamente.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ModoApresentacao {

    private static final String CODIGO_FIXO = "0000";

    private volatile boolean ativo;

    public ModoApresentacao(@Value("${apresentacao.modo:false}") boolean inicial) {
        this.ativo = inicial;

        if (inicial) {
            log.warn("MODO APRESENTACAO LIGADO NA PARTIDA, pela variavel MODO_APRESENTACAO: "
                    + "o codigo de verificacao nasce fixo em todos os fluxos. "
                    + "Subir sem a variavel volta ao sorteio.");
        }
    }

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
                    + "e troca de e-mail. Reiniciar sem a variavel desliga.");
        } else {
            log.warn("MODO APRESENTACAO DESLIGADO: o codigo de verificacao voltou a ser sorteado.");
        }
    }
}
