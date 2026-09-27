package forcamente.api.dto;


import forcamente.api.entity.enums.FormacaoEnum;
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
        @Size(min = RegrasSenha.TAMANHO_MINIMO, message = "A senha deve ter no minimo 8 caracteres")
        @Pattern( regexp = RegrasSenha.PADRAO, message = RegrasSenha.MENSAGEM)
        String senha,

        @NotNull(message = "O papel do usuario e obrigatorio")
        PapelUsuarioEnum papel,

        @Pattern(regexp = "\\d{6}-[A-Z]/[A-Z]{2}", message = "O CREF deve estar no formato 123456-G/SP")
        String cref,
        FormacaoEnum formacao,

        @Size(max = 120, message = "A instituicao deve ter no maximo 120 caracteres")
        String instituicao,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 digitos, apenas numeros")
        String cep,

        String logradouro,

        String numero,

        String complemento,


        String bairro,

        String cidade,

        @Size(max = 2, message = "O estado deve ser a sigla com 2 letras")
        String estado,

        @NotNull(message = "O aceite dos termos de uso e obrigatorio")
        @AssertTrue(message = "E necessario aceitar os termos de uso")
        Boolean aceitouTermosUso,

        @NotNull(message = "O aceite de politica de privacidade e obrigatorio")
        @AssertTrue(message = "E necessario aceitar a politica de privacidade")
        Boolean aceitouPoliticaPrivacidade
){

        public UsuarioRequestDTO{
                nomeCompleto = nomeCompleto == null ? null : nomeCompleto.trim();
                cpf = somenteDigitos(cpf);
                email = email == null ? null : email.trim().toLowerCase();
                cep = somenteDigitos(cep);
                instituicao = instituicao == null ? null : instituicao.trim();

                if (papel == PapelUsuarioEnum.PROFESSOR){
                        cref = cref == null ? null :cref.trim().toUpperCase();

                }else {
                        cref = null;
                        formacao= null;
                        instituicao = null;

                }
        }

        @AssertTrue(message = "CREF, formacao e instituicao sao obrigatorios para professor")
        public boolean isDadosProfissionaisCoerentes() {

                if (papel != PapelUsuarioEnum.PROFESSOR) {
                        return true;
                }
                return cref != null && !cref.isBlank() && formacao != null && instituicao != null && !instituicao.isBlank();

        }

        private static String somenteDigitos(String valor) {
                return valor == null ? null : valor.replaceAll("\\D", "");
        }
}
