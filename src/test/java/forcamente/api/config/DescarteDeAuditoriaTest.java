package forcamente.api.config;

import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.service.IAuditoriaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DescarteDeAuditoria - tarefa diaria de retencao")
class DescarteDeAuditoriaTest {

    @Mock
    private IAuditoriaService auditoriaService;

    @InjectMocks
    private DescarteDeAuditoria descarteDeAuditoria;

    @Test
    @DisplayName("tendo descartado algo, o proprio descarte entra na trilha")
    void deveRegistrarQuandoDescartaAlgo() {
        when(auditoriaService.descartarAntigos()).thenReturn(12L);

        descarteDeAuditoria.descartar();

        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.EXCLUIDO, RecursoAuditoriaEnum.AUDITORIA, null, null);
    }

    @Test
    @DisplayName("sem nada a descartar, nao cria registro de ruido")
    void naoDeveRegistrarQuandoNaoDescartaNada() {
        when(auditoriaService.descartarAntigos()).thenReturn(0L);

        descarteDeAuditoria.descartar();

        verify(auditoriaService, never()).registrar(any(), any(), any(), any());
    }
}
