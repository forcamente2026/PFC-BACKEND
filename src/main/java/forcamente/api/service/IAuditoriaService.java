package forcamente.api.service;

import forcamente.api.dto.OpcaoDTO;
import forcamente.api.dto.PaginaDTO;
import forcamente.api.dto.RegistroAuditoriaResponseDTO;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IAuditoriaService {

    void registrar(AcaoAuditoriaEnum acao, RecursoAuditoriaEnum recursoTipo, UUID recursoId);

    void registrar(AcaoAuditoriaEnum acao, RecursoAuditoriaEnum recursoTipo, UUID recursoId, UUID usuarioId);

    PaginaDTO<RegistroAuditoriaResponseDTO> consultar(
            LocalDate de, LocalDate ate, AcaoAuditoriaEnum acao, int pagina, int tamanho);

    String exportarCsv(LocalDate de, LocalDate ate, AcaoAuditoriaEnum acao);

    long descartarAntigos();

    List<OpcaoDTO> listarAcoes();

    List<OpcaoDTO> listarRecursos();
}
