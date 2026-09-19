package forcamente.api.service.impl;

import forcamente.api.dto.ExercicioRequestDTO;
import forcamente.api.dto.ExercicioResponseDTO;
import forcamente.api.dto.OpcaoDTO;
import forcamente.api.entity.ExercicioEntity;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.mapper.ExercicioMapper;
import forcamente.api.repository.IExercicioRepository;
import forcamente.api.service.IExercicioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExercicioService implements IExercicioService {

    private final IExercicioRepository exercicioRepository;

    private final ExercicioMapper exercicioMapper;

    @Override
    @Transactional
    public ExercicioResponseDTO criarExercicio(ExercicioRequestDTO exercicioRequestDTO) {
        log.info("criarExercicio: {}", exercicioRequestDTO.nome());

        if (exercicioRepository.existsByNomeIgnoreCase(exercicioRequestDTO.nome())) {
            throw new ConflitoException(
                    "Ja existe um exercicio cadastrado com o nome: " + exercicioRequestDTO.nome());
        }

        var exercicioEntity = exercicioMapper.toEntity(exercicioRequestDTO);
        var exercicioSalvo = exercicioRepository.save(exercicioEntity);

        return exercicioMapper.toDTO(exercicioSalvo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExercicioResponseDTO> listarExercicios() {
        log.info("listarExercicios");

        return exercicioRepository.findAll()
                .stream()
                .map(exercicioMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExercicioResponseDTO> listarPorGrupoMuscular(GrupoMuscularEnum grupoMuscular) {
        log.info("listarPorGrupoMuscular: {}", grupoMuscular);

        return exercicioRepository.findByGrupoMuscular(grupoMuscular)
                .stream()
                .map(exercicioMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExercicioResponseDTO buscarPorId(UUID exercicioId) {
        log.info("buscarPorId: {}", exercicioId);
        return exercicioMapper.toDTO(findById(exercicioId));
    }

    @Override
    @Transactional
    public ExercicioResponseDTO atualizarExercicio(UUID exercicioId, ExercicioRequestDTO exercicioRequestDTO) {
        log.info("atualizarExercicio:{}", exercicioId);

        ExercicioEntity exercicioEntity = findById(exercicioId);

        if (exercicioEntity.getGrupoMuscular() != exercicioRequestDTO.grupoMuscular()) {
            throw new ConflitoException("O Grupo Muscular não pode ser alterado");
        }

        if (exercicioRepository.existsByNomeIgnoreCaseAndIdNot(exercicioRequestDTO.nome(), exercicioId)) {
            throw new ConflitoException("Ja existe um exercicio cadastrado com o nome: " +
                    exercicioRequestDTO.nome());
        }

        exercicioMapper.atualizarEntity(exercicioRequestDTO, exercicioEntity);
        ExercicioEntity exercicioSalvo = exercicioRepository.save(exercicioEntity);

        return exercicioMapper.toDTO(exercicioSalvo);
    }

    @Override
    @Transactional
    public void excluirExercicio(UUID exercicioId) {
        log.info("excluirExercicio: {}", exercicioId);
        exercicioRepository.delete(findById(exercicioId));
    }

    private ExercicioEntity findById(UUID exercicioId) {
        return exercicioRepository.findById(exercicioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Exercicio nao encontrado: " + exercicioId));
    }

    @Override
    public List<OpcaoDTO> listarGruposMusculares() {
        log.info("listarGruposMusculares");
        return Arrays.stream(GrupoMuscularEnum.values())
                .map(grupo -> new OpcaoDTO(grupo.name(), grupo.getDescricao()))
                .toList();
    }

    @Override
    public List<OpcaoDTO> listarNiveis() {
        log.info("listarNiveis");
        return Arrays.stream(NivelDificuldadeEnum.values())
                .map(nivel -> new OpcaoDTO(nivel.name(), nivel.getDescricao()))
                .toList();
    }
}
