package forcamente.api.dto;

import forcamente.api.validation.LimiteDeBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequestDTO(
        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        String email,

        @NotBlank(message = "O codigo e obrigatorio")
        @Pattern(regexp = "\\d{4}", message = "O codigo deve ter 4 digitos")
        String codigo,

        @NotBlank(message = "A nova senha e obrigatoria")
        @Size(min = RegrasSenha.TAMANHO_MINIMO, max = RegrasSenha.TAMANHO_MAXIMO,
                message = "A senha deve ter de 8 a 64 caracteres")
        @LimiteDeBytes(max = RegrasSenha.MAXIMO_BYTES,
                message = "A senha é longa demais: acentos e símbolos contam mais de um caractere")
        @Pattern(regexp = RegrasSenha.PADRAO, message = RegrasSenha.MENSAGEM)
        String novaSenha
) {
    public RedefinirSenhaRequestDTO {
        email = email == null ? null : email.trim().toLowerCase();
        codigo = codigo == null ? null : codigo.trim();
    }
}
