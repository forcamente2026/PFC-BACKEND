package forcamente.api.dto;


import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UsuarioRequestDTO(

        @NotBlank(message = "O nome completo e obrigatorio")
        String nomeCompleto,

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

        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter 8 dígitos, apenas números")
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
                email = email == null ? null : email.trim().toLowerCase();
                cep = somenteDigitos(cep);

                if (papel == PapelUsuarioEnum.PROFESSOR){
                        cref = cref == null ? null :cref.trim().toUpperCase();

                }else {
                        cref = null;
                        formacao= null;
                        cep = null;
                        logradouro = null;
                        numero = null;
                        complemento = null;
                        bairro = null;
                        cidade = null;
                        estado = null;

                }
        }

        @AssertTrue(message = "CREF e formacao sao obrigatorios para professor")
        public boolean isDadosProfissionaisCoerentes() {
                if (papel != PapelUsuarioEnum.PROFESSOR) {
                        return true;
                }
                return preenchido(cref) && formacao != null;
        }

        @AssertTrue(message = "O endereco completo e obrigatorio para professor")
        public boolean isEnderecoDoProfessorCompleto() {
                if (papel != PapelUsuarioEnum.PROFESSOR) {
                        return true;
                }
                return preenchido(cep) && preenchido(logradouro) && preenchido(numero) && preenchido(bairro) && preenchido(cidade) && preenchido(estado);
        }

        @AssertTrue(message = "E necessario ter 18 anos ou mais para se cadastrar")
        public boolean isMaiorDeIdade() {
                if (dataNascimento == null) {
                        return true;
                }
                return !dataNascimento.isAfter(LocalDate.now().minusYears(18));
        }
        private static boolean preenchido(String valor) {
                return valor != null && !valor.isBlank();
        }

        private static String somenteDigitos(String valor) {
                return valor == null ? null : valor.replaceAll("\\D", "");
        }
}
