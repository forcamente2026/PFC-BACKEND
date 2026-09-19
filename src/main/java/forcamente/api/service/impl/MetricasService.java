package forcamente.api.service.impl;

import forcamente.api.dto.SerieRequestDTO;
import forcamente.api.dto.VolumeGrupoMuscularDTO;
import forcamente.api.dto.VolumeTreinoRequestDTO;
import forcamente.api.dto.VolumeTreinoResponseDTO;
import forcamente.api.entity.ExercicioEntity;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.repository.IExercicioRepository;
import forcamente.api.service.IMetricasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricasService implements IMetricasService {

    private final IExercicioRepository exercicioRepository;

    @Override
    @Transactional(readOnly = true)
    public VolumeTreinoResponseDTO calcularVolumeTreino(VolumeTreinoRequestDTO volumeTreinoRequestDTO) {

        log.info("calcularVolumeTreino: {} series", volumeTreinoRequestDTO.series().size());

        List<UUID> ids = volumeTreinoRequestDTO.series()
                .stream()
                .map(SerieRequestDTO::exercicioId)
                .distinct()
                .toList();

        Map<UUID, ExercicioEntity> exercicios = exercicioRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(ExercicioEntity::getId, exercicio -> exercicio));

        if (exercicios.size() < ids.size()) {
            throw new RecursoNaoEncontradoException("Algum exercício informado não existe");
        }

        double volumeTotalKg = 0;
        int totalSeries = 0;
        int totalRepeticoes = 0;
        Map<GrupoMuscularEnum, Double> volumePorGrupo = new LinkedHashMap<>();

        for (SerieRequestDTO serie : volumeTreinoRequestDTO.series()) {
            ExercicioEntity exercicio = exercicios.get(serie.exercicioId());
            double volume = serie.series() * serie.repeticoes() * serie.cargaKg();

            volumeTotalKg += volume;
            totalSeries += serie.series();
            totalRepeticoes += serie.series() * serie.repeticoes();
            volumePorGrupo.merge(exercicio.getGrupoMuscular(), volume, Double::sum);
        }

        int intensidade = totalRepeticoes > 0
                ? (int) Math.round(volumeTotalKg / totalRepeticoes)
                : 0;

        List<VolumeGrupoMuscularDTO> porGrupo = volumePorGrupo.entrySet()
                .stream()
                .map(entrada -> new VolumeGrupoMuscularDTO(
                        entrada.getKey(),
                        entrada.getKey().getDescricao(),
                        arredondar(entrada.getValue())))
                .toList();

        return new VolumeTreinoResponseDTO(
                arredondar(volumeTotalKg),
                porGrupo,
                totalSeries,
                totalRepeticoes,
                intensidade);
    }

            private double arredondar(double valor){
                return Math.round(valor * 100.0) / 100.0;
            }
        }
