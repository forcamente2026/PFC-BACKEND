package forcamente.api.config;

import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.service.IAuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DescarteDeAuditoria {

    private final IAuditoriaService auditoriaService;

    @Scheduled(cron = "0 0 3 * * *")
    public void descartar() {
        log.info("Iniciando descarte dos registros de auditoria fora do prazo de retencao");

        long descartados = auditoriaService.descartarAntigos();

        if (descartados > 0) {
            auditoriaService.registrar(
                    AcaoAuditoriaEnum.EXCLUIDO, RecursoAuditoriaEnum.AUDITORIA, null, null);
        }
    }
}
