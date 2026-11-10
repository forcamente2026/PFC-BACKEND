package forcamente.api.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Envia e-mail de verdade. Habilitar manualmente, trocar o destinatario e rodar sozinho.")
@DisplayName("Envio real de e-mail - verificacao manual")
class EnvioRealDeEmailTest {

    @Autowired
    private IEmailService emailService;

    @Test
    @DisplayName("deve enviar um e-mail de teste para o destinatario informado")
    void deveEnviarEmailDeVerdade() {
        emailService.enviar(
                "troque-pelo-seu-email@gmail.com",
                "ForcaMente - teste de envio",
                "Se voce recebeu esta mensagem, a configuracao de e-mail esta correta.");
    }
}