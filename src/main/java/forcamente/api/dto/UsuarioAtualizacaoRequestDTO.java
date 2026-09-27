package forcamente.api.dto;

import forcamente.api.entity.enums.FormacaoEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioAtualizacaoRequestDTO(

        @NotBlank(message = "O nome completo e obrigatorio")
        String nomeCompleto,

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        String email,

        @Pattern(regexp = "\\d{6}-[A-Z]/[A-Z]{2}", message = "O CREF deve estar no formato 123456-G/SP")
        String cref,

        FormacaoEnum formacao,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 dígitos, apenas números")
        String cep,

        String logradouro,

        String numero,

        String complemento,

        String bairro,

        String cidade,

        @Size(max = 2, message = "O estado deve ser a sigla com 2 letras")
        String estado
) {

        public UsuarioAtualizacaoRequestDTO {
                nomeCompleto = aparar(nomeCompleto);
                email = email == null ? null : email.trim().toLowerCase();
                cref = cref == null ? null : cref.trim().toUpperCase();
                cep = cep == null ? null : cep.replaceAll("\\D", "");
                logradouro = aparar(logradouro);
                numero = aparar(numero);
                complemento = aparar(complemento);
                bairro = aparar(bairro);
                cidade = aparar(cidade);
                estado = estado == null ? null : estado.trim().toUpperCase();
        }

        private static String aparar(String valor) {
                if (valor == null) {
                        return null;
                }
                String limpo = valor.trim();
                return limpo.isEmpty() ? null : limpo;
        }
}
