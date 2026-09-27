package forcamente.api.mapper;

import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsuarioMapper - conversao entre DTO e entidade")
class UsuarioMapperTest {

    private final UsuarioMapper mapper = Mappers.getMapper(UsuarioMapper.class);

    @Test
    @DisplayName("deve copiar nascimento, CREF, formacao e instituicao")
    void deveMapearRequestParaEntity() {
        var request = new UsuarioRequestDTO(
                "Maria Souza", "98765432100", LocalDate.of(1990, 5, 20), "maria@umc.br", "Senha@123",
                PapelUsuarioEnum.PROFESSOR, "123456-G/SP", FormacaoEnum.BACHARELADO, "Universidade de Mogi das Cruzes",
                "08780000", "Rua B", "2", null, "Centro", "Mogi das Cruzes", "SP", true, true);

        var entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getSenhaHash()).isNull();
        assertThat(entity.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(entity.getCref()).isEqualTo("123456-G/SP");
        assertThat(entity.getFormacao()).isEqualTo(FormacaoEnum.BACHARELADO);
        assertThat(entity.getInstituicao()).isEqualTo("Universidade de Mogi das Cruzes");
        assertThat(entity.isAtivo()).isTrue();
        assertThat(entity.getCriadoEm()).isNotNull();
        assertThat(entity.getAceitouTermosUsoEm()).isNull();
        assertThat(entity.getVersaoTermosUso()).isNull();
    }

    @Test
    @DisplayName("deve devolver CREF e categoria na resposta e nunca CPF nem senha")
    void deveMapearEntityParaResponse() {
        var entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNomeCompleto("Maria Souza");
        entity.setCpf("98765432100");
        entity.setEmail("maria@umc.br");
        entity.setSenhaHash("$2a$10$hash");
        entity.setPapel(PapelUsuarioEnum.PROFESSOR);
        entity.setDataNascimento(LocalDate.of(1990, 5, 20));
        entity.setCref("123456-G/SP");
        entity.setFormacao(FormacaoEnum.LICENCIATURA);
        entity.setInstituicao("Universidade de Mogi das Cruzes");
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.now());

        var response = mapper.toDTO(entity);

        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.cref()).isEqualTo("123456-G/SP");
        assertThat(response.formacao()).isEqualTo(FormacaoEnum.LICENCIATURA);
        assertThat(response.instituicao()).isEqualTo("Universidade de Mogi das Cruzes");
        assertThat(response).hasNoNullFieldsOrPropertiesExcept("cidade", "estado");
    }
}