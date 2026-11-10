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
    @DisplayName("deve copiar nascimento, CREF e formacao")
    void deveMapearRequestParaEntity() {
        var request = new UsuarioRequestDTO(
                "Maria Souza", LocalDate.of(1990, 5, 20), "maria@umc.br", "Senha@123",
                PapelUsuarioEnum.PROFESSOR, "123456-G/SP", FormacaoEnum.BACHARELADO,
                "08780000", "Rua B", "2", null, "Centro", "Mogi das Cruzes", "SP", true, true);

        var entity = mapper.toEntity(request);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getSenhaHash()).isNull();
        assertThat(entity.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 20));
        assertThat(entity.getCref()).isEqualTo("123456-G/SP");
        assertThat(entity.getFormacao()).isEqualTo(FormacaoEnum.BACHARELADO);
        assertThat(entity.isAtivo()).isTrue();
        assertThat(entity.getCriadoEm()).isNotNull();
        assertThat(entity.getAceitouTermosUsoEm()).isNull();
        assertThat(entity.getVersaoTermosUso()).isNull();
    }

    @Test
    @DisplayName("deve devolver CREF e formacao na resposta e nunca a senha")
    void deveMapearEntityParaResponse() {
        var entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNomeCompleto("Maria Souza");
        entity.setEmail("maria@umc.br");
        entity.setSenhaHash("$2a$10$hash");
        entity.setPapel(PapelUsuarioEnum.PROFESSOR);
        entity.setDataNascimento(LocalDate.of(1990, 5, 20));
        entity.setCref("123456-G/SP");
        entity.setFormacao(FormacaoEnum.LICENCIATURA);
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.now());

        var response = mapper.toDTO(entity);

        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.cref()).isEqualTo("123456-G/SP");
        assertThat(response.formacao()).isEqualTo(FormacaoEnum.LICENCIATURA);
        assertThat(response).hasNoNullFieldsOrPropertiesExcept("cidade", "estado");
    }
}