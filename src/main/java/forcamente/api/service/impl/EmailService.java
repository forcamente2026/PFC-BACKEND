package forcamente.api.service.impl;


import forcamente.api.service.IEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService implements IEmailService {

    private final JavaMailSender enviadorDeEmail;

    private final String remetente;

    public EmailService(JavaMailSender enviadorDeEmail, @Value("${spring.mail.username:}") String remetente) {
        this.enviadorDeEmail = enviadorDeEmail;
        this.remetente = remetente;
    }

    @Override
    public void enviar(String destinatario, String assunto, String texto){
    var mensagem = new SimpleMailMessage();
    mensagem.setFrom(remetente);
    mensagem.setTo(destinatario);
    mensagem.setSubject(assunto);
    mensagem.setText(texto);

    enviadorDeEmail.send(mensagem);

    log.info("Email enviado para: {}", assunto);

    }

    @Override
    @Async
    public void enviarAssincrono(String destinatario, String assunto, String texto){
        enviar(destinatario, assunto, texto);
    }
}
