package forcamente.api.dto;

import forcamente.api.entity.enums.CategoriaProfissionalEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsuarioRequestDTO - normalizacao e validacao do cadastro")
class UsuarioRequestDTOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("deve aceitar aluno valido e normalizar CPF, CEP e e-mail")
    void deveAceitarAlunoValidoENormalizar() {
        var dto = umAluno("123.456.789-01", "  Joao@UMC.br ", "08780-000", "Senha@123");

        assertThat(dto.cpf()).isEqualTo("12345678901");
        assertThat(dto.email()).isEqualTo("joao@umc.br");
        assertThat(dto.cep()).isEqualTo("08780000");
        assertThat(validar(dto)).isEmpty();
    }

    @Test
    @DisplayName("deve rejeitar senha sem maiuscula, numero ou especial")
    void deveRejeitarSenhaFraca() {
        var dto = umAluno("12345678901", "joao@umc.br", "08780000", "senhafraca");

        assertThat(campos(validar(dto))).contains("senha");
    }

    @Test
    @DisplayName("deve rejeitar senha sem minuscula")
    void deveRejeitarSenhaSemMinuscula() {
        var dto = umAluno("12345678901", "joao@umc.br", "08780000", "SENHA@123");

        assertThat(campos(validar(dto))).contains("senha");
    }

    @Test
    @DisplayName("deve rejeitar professor sem CREF e categoria")
    void deveRejeitarProfessorSemCref() {
        var dto = umProfessor(null, null);

        assertThat(campos(validar(dto))).contains("dadosProfissionaisCoerentes");
    }

    @Test
    @DisplayName("deve rejeitar CREF fora do formato 123456-G/SP")
    void deveRejeitarCrefForaDoFormato() {
        var dto = umProfessor("12345-G/SP", CategoriaProfissionalEnum.TREINAMENTO_ESPORTIVO);

        assertThat(campos(validar(dto))).contains("cref");
    }

    @Test
    @DisplayName("deve aceitar professor com CREF valido e normalizar para maiusculas")
    void deveAceitarProfessorValido() {
        var dto = umProfessor(" 123456-g/sp ", CategoriaProfissionalEnum.SAUDE_E_REABILITACAO);

        assertThat(dto.cref()).isEqualTo("123456-G/SP");
        assertThat(validar(dto)).isEmpty();
    }

    @Test
    @DisplayName("deve descartar CREF e categoria quando o papel e aluno")
    void deveDescartarDadosProfissionaisDeAluno() {
        var dto = new UsuarioRequestDTO(
                "Joao da Silva", "12345678901", LocalDate.of(2000, 1, 1), "joao@umc.br",
                "Senha@123", PapelUsuarioEnum.ALUNO,
                "123456-G/SP", CategoriaProfissionalEnum.TREINAMENTO_ESPORTIVO,
                "08780000", "Rua A", "1", null, "Centro", "Mogi das Cruzes", "SP");

        assertThat(dto.cref()).isNull();
        assertThat(dto.categoriaProfissional()).isNull();
        assertThat(validar(dto)).isEmpty();
    }

    private Set<ConstraintViolation<UsuarioRequestDTO>> validar(UsuarioRequestDTO dto) {
        return validator.validate(dto);
    }

    private Set<String> campos(Set<ConstraintViolation<UsuarioRequestDTO>> violacoes) {
        return violacoes.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    private UsuarioRequestDTO umAluno(String cpf, String email, String cep, String senha) {
        return new UsuarioRequestDTO(
                "Joao da Silva", cpf, LocalDate.of(2000, 1, 1), email, senha,
                PapelUsuarioEnum.ALUNO, null, null,
                cep, "Rua A", "1", null, "Centro", "Mogi das Cruzes", "SP");
    }

    private UsuarioRequestDTO umProfessor(String cref, CategoriaProfissionalEnum categoria) {
        return new UsuarioRequestDTO(
                "Maria Souza", "98765432100", LocalDate.of(1990, 5, 20), "maria@umc.br", "Senha@123",
                PapelUsuarioEnum.PROFESSOR, cref, categoria,
                "08780000", "Rua B", "2", null, "Centro", "Mogi das Cruzes", "SP");
    }
}