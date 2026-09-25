package forcamente.api.service;

import forcamente.api.dto.ExercicioRequestDTO;
import forcamente.api.dto.ExercicioResponseDTO;
import forcamente.api.entity.ExercicioEntity;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.mapper.ExercicioMapper;
import forcamente.api.repository.IExercicioRepository;
import forcamente.api.service.impl.ExercicioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExercicioService - regras de cadastro e listagem de exercicios")
class ExercicioServiceTest {

    @Mock
    private IExercicioRepository exercicioRepository;

    @Mock
    private ExercicioMapper exercicioMapper;

    @InjectMocks
    private ExercicioService exercicioService;



    @Test
    @DisplayName("deve cadastrar exercicio novo e devolver o DTO de resposta")
    void deveCadastrarExercicioNovo() {

        var requestDTO = umaRequisicaoValida();
        var entity = umaEntidade();
        var responseDTO = umaResposta(entity);

        when(exercicioRepository.existsByNomeIgnoreCase("Supino reto")).thenReturn(false);
        when(exercicioMapper.toEntity(requestDTO)).thenReturn(entity);
        when(exercicioRepository.save(entity)).thenReturn(entity);
        when(exercicioMapper.toDTO(entity)).thenReturn(responseDTO);

        var resultado = exercicioService.criarExercicio(requestDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.nome()).isEqualTo("Supino reto");
        assertThat(resultado.grupoMuscular()).isEqualTo(GrupoMuscularEnum.PEITO);
        assertThat(resultado.nivel()).isEqualTo(NivelDificuldadeEnum.INICIANTE);

        verify(exercicioRepository).save(entity);
    }

    @Test
    @DisplayName("nao deve cadastrar exercicio com nome ja existente")
    void naoDeveCadastrarExercicioComNomeDuplicado() {
        var requestDTO = umaRequisicaoValida();

        when(exercicioRepository.existsByNomeIgnoreCase("Supino reto")).thenReturn(true);

        assertThatThrownBy(() -> exercicioService.criarExercicio(requestDTO))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Ja existe um exercicio cadastrado");

        verify(exercicioRepository, never()).save(any(ExercicioEntity.class));
    }

    @Test
    @DisplayName("deve listar todos os exercicios convertidos para DTO")
    void deveListarTodosOsExercicios() {
        var entity = umaEntidade();
        var responseDTO = umaResposta(entity);

        when(exercicioRepository.findAll()).thenReturn(List.of(entity));
        when(exercicioMapper.toDTO(entity)).thenReturn(responseDTO);

        List<ExercicioResponseDTO> resultado = exercicioService.listarExercicios();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().nome()).isEqualTo("Supino reto");
    }

    @Test
    @DisplayName("deve filtrar exercicios por grupo muscular")
    void deveListarPorGrupoMuscular() {
        var entity = umaEntidade();
        var responseDTO = umaResposta(entity);

        when(exercicioRepository.findByGrupoMuscular(GrupoMuscularEnum.PEITO))
                .thenReturn(List.of(entity));
        when(exercicioMapper.toDTO(entity)).thenReturn(responseDTO);

        var resultado = exercicioService.listarPorGrupoMuscular(GrupoMuscularEnum.PEITO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().grupoMuscular()).isEqualTo(GrupoMuscularEnum.PEITO);
    }

    @Test
    @DisplayName("deve lancar excecao ao buscar exercicio inexistente")
    void deveLancarExcecaoQuandoExercicioNaoExiste() {
        var idInexistente = UUID.randomUUID();

        when(exercicioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exercicioService.buscarPorId(idInexistente))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Exercicio nao encontrado");
    }

    private ExercicioRequestDTO umaRequisicaoValida() {
        return new ExercicioRequestDTO(
                "Supino reto",
                GrupoMuscularEnum.PEITO,
                NivelDificuldadeEnum.INICIANTE,
                "Deite no banco, retraia as escapulas e desca a barra ate o peito.",
                "Abrir demais os cotovelos e quicar a barra no peito.",
                "Rotacao de ombros e 1 serie leve de aquecimento.",
                "Barra e banco reto",
                "https://exemplo.com/supino.gif");
    }

    private ExercicioEntity umaEntidade() {
        var entity = new ExercicioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNome("Supino reto");
        entity.setGrupoMuscular(GrupoMuscularEnum.PEITO);
        entity.setNivel(NivelDificuldadeEnum.INICIANTE);
        entity.setDescricaoExecucao("Deite no banco, retraia as escapulas e desca a barra ate o peito.");
        entity.setCriadoEm(LocalDateTime.now());
        return entity;
    }

    private ExercicioResponseDTO umaResposta(ExercicioEntity entity) {
        return new ExercicioResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getGrupoMuscular(),
                entity.getNivel(),
                entity.getDescricaoExecucao(),
                entity.getErrosComuns(),
                entity.getAquecimentoRecomendado(),
                entity.getEquipamento(),
                entity.getGifUrl(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm());
    }
}
