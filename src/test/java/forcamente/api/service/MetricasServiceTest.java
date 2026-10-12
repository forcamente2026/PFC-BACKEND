package forcamente.api.service;

import forcamente.api.dto.GastoEnergeticoRequestDTO;
import forcamente.api.dto.HidratacaoRequestDTO;
import forcamente.api.dto.SerieRequestDTO;
import forcamente.api.dto.VolumeTreinoRequestDTO;
import forcamente.api.entity.ExercicioEntity;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelAtividadeEnum;
import forcamente.api.entity.enums.SexoBiologicoEnum;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.repository.IExercicioRepository;
import forcamente.api.service.impl.MetricasService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricasService - volume de treino e calculadoras")
class MetricasServiceTest {

    private static final UUID SUPINO = UUID.randomUUID();
    private static final UUID CRUCIFIXO = UUID.randomUUID();
    private static final UUID AGACHAMENTO = UUID.randomUUID();

    @Mock
    private IExercicioRepository exercicioRepository;

    @InjectMocks
    private MetricasService metricasService;

    @Test
    @DisplayName("calcula o volume agrupado por grupo muscular, conforme a CA01 do RF16")
    void deveCalcularVolumeConformeCA01() {
        when(exercicioRepository.findAllById(any())).thenReturn(List.of(
                umExercicio(SUPINO, GrupoMuscularEnum.PEITO),
                umExercicio(CRUCIFIXO, GrupoMuscularEnum.PEITO),
                umExercicio(AGACHAMENTO, GrupoMuscularEnum.PERNA)));

        var resposta = metricasService.calcularVolumeTreino(new VolumeTreinoRequestDTO(List.of(
                new SerieRequestDTO(SUPINO, 4, 10, 60.0),
                new SerieRequestDTO(CRUCIFIXO, 3, 12, 14.0),
                new SerieRequestDTO(AGACHAMENTO, 4, 8, 80.0))));

        assertThat(resposta.volumeTotalKg()).isEqualTo(5464.0);
        assertThat(resposta.totalSeries()).isEqualTo(11);
        assertThat(resposta.totalRepeticoes()).isEqualTo(108);
        assertThat(resposta.intensidadeMediaKgPorRep()).isEqualTo(51);
        assertThat(resposta.volumePorGrupoMuscular())
                .extracting("grupoMuscular", "volumeKg")
                .containsExactly(
                        tuple(GrupoMuscularEnum.PEITO, 2904.0),
                        tuple(GrupoMuscularEnum.PERNA, 2560.0));
    }

    @Test
    @DisplayName("aceita carga zero, para exercício com peso corporal")
    void deveAceitarCargaZero() {
        when(exercicioRepository.findAllById(any())).thenReturn(List.of(
                umExercicio(SUPINO, GrupoMuscularEnum.ABDOMEN)));

        var resposta = metricasService.calcularVolumeTreino(new VolumeTreinoRequestDTO(
                List.of(new SerieRequestDTO(SUPINO, 3, 15, 0.0))));

        assertThat(resposta.volumeTotalKg()).isZero();
        assertThat(resposta.totalRepeticoes()).isEqualTo(45);
        assertThat(resposta.intensidadeMediaKgPorRep()).isZero();
    }

    @Test
    @DisplayName("rejeita a requisição inteira quando algum exercício não existe")
    void deveRejeitarExercicioInexistente() {
        when(exercicioRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> metricasService.calcularVolumeTreino(new VolumeTreinoRequestDTO(
                List.of(new SerieRequestDTO(SUPINO, 4, 10, 60.0)))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Algum exercício informado não existe");
    }

    @Test
    @DisplayName("busca os exercícios em uma única consulta, sem usar findById")
    void deveBuscarEmUmaConsultaSo() {
        when(exercicioRepository.findAllById(any())).thenReturn(List.of(
                umExercicio(SUPINO, GrupoMuscularEnum.PEITO),
                umExercicio(CRUCIFIXO, GrupoMuscularEnum.PEITO)));

        metricasService.calcularVolumeTreino(new VolumeTreinoRequestDTO(List.of(
                new SerieRequestDTO(SUPINO, 4, 10, 60.0),
                new SerieRequestDTO(CRUCIFIXO, 3, 12, 14.0))));

        verify(exercicioRepository).findAllById(any());
        verify(exercicioRepository, never()).findById(any());
    }

    @Test
    @DisplayName("calcula o gasto energético masculino por Mifflin-St Jeor")
    void deveCalcularGastoEnergeticoMasculino() {
        var resposta = metricasService.calcularGastoEnergetico(new GastoEnergeticoRequestDTO(
                80.0, 180, 30, SexoBiologicoEnum.MASCULINO, NivelAtividadeEnum.MODERADO));

        assertThat(resposta.tmb()).isEqualTo(1780);
        assertThat(resposta.gastoEnergeticoTotal()).isEqualTo(2759);
        assertThat(resposta.fatorAtividadeAplicado()).isEqualTo(1.55);
        assertThat(resposta.formulaUtilizada()).isEqualTo("Mifflin-St Jeor");
    }

    @Test
    @DisplayName("subtrai 161 no feminino e aplica o fator sobre o TMB não arredondado")
    void deveCalcularGastoEnergeticoFeminino() {
        var resposta = metricasService.calcularGastoEnergetico(new GastoEnergeticoRequestDTO(
                60.0, 165, 25, SexoBiologicoEnum.FEMININO, NivelAtividadeEnum.LEVE));

        assertThat(resposta.tmb()).isEqualTo(1345);
        assertThat(resposta.gastoEnergeticoTotal()).isEqualTo(1850);
    }

    @Test
    @DisplayName("calcula a hidratação somando as parcelas já arredondadas")
    void deveCalcularHidratacao() {
        var resposta = metricasService.calcularHidratacao(new HidratacaoRequestDTO(72.5, 60));

        assertThat(resposta.baseCorporalMl()).isEqualTo(2538);
        assertThat(resposta.adicionalTreinoMl()).isEqualTo(500);
        assertThat(resposta.mlPorDia()).isEqualTo(3038);
        assertThat(resposta.litrosPorDia()).isEqualTo(3.04);
    }

    @Test
    @DisplayName("expõe os cinco níveis de atividade e os dois sexos como código e descrição")
    void deveListarOpcoes() {
        assertThat(metricasService.listarNiveisAtividade()).hasSize(5);
        assertThat(metricasService.listarNiveisAtividade().getFirst().codigo())
                .isEqualTo("SEDENTARIO");
        assertThat(metricasService.listarNiveisAtividade().getFirst().descricao())
                .isEqualTo("Sedentário, sem treino regular");
        assertThat(metricasService.listarSexosBiologicos()).hasSize(2);
        assertThat(metricasService.listarSexosBiologicos().getFirst().codigo())
                .isEqualTo("MASCULINO");
    }

    @Test
    @DisplayName("mantém os cinco fatores de atividade da regra de negócio")
    void deveManterOsFatoresDeAtividade() {
        assertThat(NivelAtividadeEnum.SEDENTARIO.getFator()).isEqualTo(1.2);
        assertThat(NivelAtividadeEnum.LEVE.getFator()).isEqualTo(1.375);
        assertThat(NivelAtividadeEnum.MODERADO.getFator()).isEqualTo(1.55);
        assertThat(NivelAtividadeEnum.INTENSO.getFator()).isEqualTo(1.725);
        assertThat(NivelAtividadeEnum.MUITO_INTENSO.getFator()).isEqualTo(1.9);
    }

    private ExercicioEntity umExercicio(UUID id, GrupoMuscularEnum grupo) {
        var exercicio = new ExercicioEntity();
        exercicio.setId(id);
        exercicio.setGrupoMuscular(grupo);
        return exercicio;
    }
}