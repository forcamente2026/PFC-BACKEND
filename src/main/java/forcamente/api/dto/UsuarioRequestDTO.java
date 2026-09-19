package forcamente.api.dto;

import forcamente.api.entity.enums.CategoriaProfissionalEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UsuarioRequestDTO(

        @NotBlank(message = "O nome completo e obrigatorio")
        String nomeCompleto,

        @NotBlank(message = "O CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter 11 digitos, apenas numeros")
        String cpf,

        @NotNull(message ="A data de nascimento e obrigatoria")
        @Past(message = "A data de nascimento deve estar no passado")
        LocalDate dataNascimento,

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        String email,

        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 8, message = "A senha deve ter no minimo 8 caracteres")
        @Pattern(
                regexp = "(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\p{N})(?=.*[^\\p{L}\\p{N}\\s]).*",
                message = "A senha deve ter letra minuscula, maiuscula, numero e caractere especial"
        )
        String senha,

        @NotNull(message = "O papel do usuario e obrigatorio")
        PapelUsuarioEnum papel,

        @Pattern(regexp = "\\d{6}-[A-Z]/[A-Z]{2}", message = "O CREF deve estar no formato 123456-G/SP")
        String cref,
        CategoriaProfissionalEnum categoriaProfissional,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 digitos, apenas numeros")
        String cep,

        String logradouro,

        String numero,

        String complemento,


        String bairro,

        String cidade,

        @Size(max = 2, message = "O estado deve ser a sigla com 2 letras")
        String estado
) {
        public UsuarioRequestDTO{
                nomeCompleto = nomeCompleto == null ? null : nomeCompleto.trim();
                cpf = somenteDigitos(cpf);
                email = email == null ? null : email.trim().toLowerCase();
                cep = somenteDigitos(cep);

                if (papel == PapelUsuarioEnum.PROFESSOR){
                        cref = cref == null ? null :cref.trim().toUpperCase();

                }else {
                        cref = null;
                        categoriaProfissional= null;

                }
        }

        @AssertTrue(message = "CREF e categoria profissional sao obrigatorios para professor")
        public boolean isDadosProfissionaisCoerentes() {
                if (papel != PapelUsuarioEnum.PROFESSOR) {
                        return true;
                }
                return cref != null && !cref.isBlank() && categoriaProfissional() != null;

        }

        private static String somenteDigitos(String valor) {
                return valor == null ? null : valor.replaceAll("\\D", "");
        }
}
