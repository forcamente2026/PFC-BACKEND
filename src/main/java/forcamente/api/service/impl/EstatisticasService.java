package forcamente.api.service.impl;

import forcamente.api.dto.AtividadePorDiaResponseDTO;
import forcamente.api.dto.ContasPorPapelResponseDTO;
import forcamente.api.entity.RegistroAuditoriaEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.repository.IRegistroAuditoriaRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IEstatisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class EstatisticasService implements IEstatisticasService {

    private static final int DIAS_MINIMO = 1;

    private static final int DIAS_MAXIMO = 90;

    private final IUsuarioRepository usuarioRepository;

    private final IRegistroAuditoriaRepository registroAuditoriaRepository;

    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ContasPorPapelResponseDTO> contarContasPorPapel() {
        return Arrays.stream(PapelUsuarioEnum.values())
                .map(papel -> new ContasPorPapelResponseDTO(
                        papel, usuarioRepository.countByPapelAndAnonimizadoEmIsNull(papel)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtividadePorDiaResponseDTO> contarAtividadePorDia(int dias) {
        if (dias < DIAS_MINIMO || dias > DIAS_MAXIMO) {
            throw new RegraDeNegocioException(
                    "O numero de dias deve estar entre " + DIAS_MINIMO + " e " + DIAS_MAXIMO);
        }

        LocalDate hoje = LocalDate.now(clock);
        LocalDate primeiroDia = hoje.minusDays(dias - 1L);

        Map<LocalDate, Long> totalPorDia = registroAuditoriaRepository
                .findByOcorridoEmGreaterThanEqualAndAcaoNot(
                        primeiroDia.atStartOfDay(), AcaoAuditoriaEnum.LIDO)
                .stream()
                .collect(Collectors.groupingBy(
                        this::diaDoRegistro, Collectors.counting()));

        return IntStream.range(0, dias)
                .mapToObj(deslocamento -> primeiroDia.plusDays(deslocamento))
                .map(dia -> new AtividadePorDiaResponseDTO(dia, totalPorDia.getOrDefault(dia, 0L)))
                .toList();
    }

    private LocalDate diaDoRegistro(RegistroAuditoriaEntity registro) {
        return registro.getOcorridoEm().toLocalDate();
    }
}
