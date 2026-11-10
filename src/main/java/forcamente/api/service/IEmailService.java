package forcamente.api.service;

public interface IEmailService {
    void enviar(String destinatario, String assunto, String texto);
    void enviarAssincrono(String destinatario, String assunto, String texto);
}
