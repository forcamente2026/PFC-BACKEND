package forcamente.api.service;

import forcamente.api.service.impl.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - montagem e envio da mensagem")
class EmailServiceTest {

    @Mock
    private JavaMailSender enviadorDeEmail;

    private EmailService emailService;

    @BeforeEach
    void configurar() {
        emailService = new EmailService(enviadorDeEmail, "forcamente@gmail.com");
    }

    @Test
    @DisplayName("deve montar a mensagem com remetente, destinatario, assunto e texto")
    void deveMontarEEnviarAMensagem() {
        emailService.enviar("aluno@umc.br", "Codigo de acesso", "Seu codigo e 1234");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(enviadorDeEmail).send(captor.capture());
        var mensagem = captor.getValue();

        assertThat(mensagem.getFrom()).isEqualTo("forcamente@gmail.com");
        assertThat(mensagem.getTo()).containsExactly("aluno@umc.br");
        assertThat(mensagem.getSubject()).isEqualTo("Codigo de acesso");
        assertThat(mensagem.getText()).contains("1234");
    }

    @Test
    @DisplayName("o envio assincrono usa o mesmo caminho de montagem")
    void deveDelegarOEnvioAssincrono() {
        emailService.enviarAssincrono("aluno@umc.br", "Assunto", "Texto");

        verify(enviadorDeEmail).send(any(SimpleMailMessage.class));
    }
}