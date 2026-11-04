package forcamente.api.dto;

import forcamente.api.validation.LimiteDeBytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequestDTO(

        @NotBlank(message = "Informe a senha atual")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = RegrasSenha.TAMANHO_MINIMO, max = RegrasSenha.TAMANHO_MAXIMO,
                message = "A senha deve ter de 8 a 64 caracteres")
        @LimiteDeBytes(max = RegrasSenha.MAXIMO_BYTES,
                message = "A senha é longa demais: acentos e símbolos contam mais de um caractere")
        @Pattern(regexp = RegrasSenha.PADRAO, message = RegrasSenha.MENSAGEM)
        String novaSenha
) {
}
