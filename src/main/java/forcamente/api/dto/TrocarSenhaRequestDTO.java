package forcamente.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequestDTO(

        @NotBlank(message = "Informe a senha atual")
        String senhaAtual,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = RegrasSenha.TAMANHO_MINIMO, message = "A senha deve ter no minimo 8 caracteres")
        @Pattern(regexp = RegrasSenha.PADRAO, message = RegrasSenha.MENSAGEM)
        String novaSenha
) {
}
