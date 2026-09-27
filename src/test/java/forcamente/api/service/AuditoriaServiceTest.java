package forcamente.api.service;

import forcamente.api.entity.RegistroAuditoriaEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.repository.IRegistroAuditoriaRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.AuditoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaService - gravacao da trilha")
class AuditoriaServiceTest {

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-09-27T12:00:00Z"), ZoneOffset.UTC);

    private static final int RETENCAO_MESES = 6;

    @Mock
    private IRegistroAuditoriaRepository registroAuditoriaRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    private AuditoriaService auditoriaService;

    @BeforeEach
    void configurar() {
        auditoriaService = new AuditoriaService(
                registroAuditoriaRepository, usuarioRepository, RELOGIO, RETENCAO_MESES);
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("deve gravar acao, recurso e o momento do relogio")
    void deveGravarORegistro() {
        var recursoId = UUID.randomUUID();

        auditoriaService.registrar(
                AcaoAuditoriaEnum.CRIADO, RecursoAuditoriaEnum.EXERCICIO, recursoId);

        var salvo = capturar();
        assertThat(salvo.getAcao()).isEqualTo(AcaoAuditoriaEnum.CRIADO);
        assertThat(salvo.getRecursoTipo()).isEqualTo(RecursoAuditoriaEnum.EXERCICIO);
        assertThat(salvo.getRecursoId()).isEqualTo(recursoId);
        assertThat(salvo.getOcorridoEm()).isEqualTo(LocalDateTime.now(RELOGIO));
    }

    @Test
    @DisplayName("deve descobrir sozinho quem esta autenticado, pelo sub do token")
    void deveUsarOUsuarioAutenticado() {
        var usuarioId = UUID.randomUUID();
        autenticarComo(usuarioId);

        auditoriaService.registrar(
                AcaoAuditoriaEnum.LIDO, RecursoAuditoriaEnum.USUARIO, usuarioId);

        assertThat(capturar().getUsuarioId()).isEqualTo(usuarioId);
    }

    @Test
    @DisplayName("sem ninguem autenticado, o usuario fica nulo")
    void deveGravarSemUsuarioQuandoNaoHaAutenticacao() {
        auditoriaService.registrar(
                AcaoAuditoriaEnum.LOGIN_FALHOU, RecursoAuditoriaEnum.USUARIO, null);

        assertThat(capturar().getUsuarioId()).isNull();
    }

    @Test
    @DisplayName("o usuario informado explicitamente prevalece sobre o contexto")
    void deveUsarOUsuarioInformado() {
        autenticarComo(UUID.randomUUID());
        var informado = UUID.randomUUID();

        auditoriaService.registrar(
                AcaoAuditoriaEnum.LOGIN_REALIZADO, RecursoAuditoriaEnum.USUARIO, informado, informado);

        assertThat(capturar().getUsuarioId()).isEqualTo(informado);
    }

    @Test
    @DisplayName("deve resolver nome e e-mail do usuario numa consulta so, em lote")
    void deveResolverNomeEEmailEmLote() {
        var usuarioId = UUID.randomUUID();
        var registroA = umRegistro(AcaoAuditoriaEnum.CRIADO, usuarioId);
        var registroB = umRegistro(AcaoAuditoriaEnum.LIDO, usuarioId);

        when(registroAuditoriaRepository.findByOcorridoEmBetweenOrderByOcorridoEmDesc(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(registroA, registroB)));
        when(usuarioRepository.findAllById(List.of(usuarioId)))
                .thenReturn(List.of(umUsuario(usuarioId, "Maria Souza", "maria@umc.br")));

        var pagina = auditoriaService.consultar(null, null, null, 0, 50);

        assertThat(pagina.conteudo()).hasSize(2);
        assertThat(pagina.conteudo().getFirst().usuarioNome()).isEqualTo("Maria Souza");
        assertThat(pagina.conteudo().getFirst().usuarioEmail()).isEqualTo("maria@umc.br");
        assertThat(pagina.conteudo().getFirst().descricaoAcao()).isEqualTo("Criado");
        verify(usuarioRepository).findAllById(List.of(usuarioId));
    }

    @Test
    @DisplayName("registro sem usuario devolve nome e e-mail nulos, sem consultar o banco")
    void deveDevolverUsuarioNuloQuandoNaoHa() {
        when(registroAuditoriaRepository.findByOcorridoEmBetweenOrderByOcorridoEmDesc(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(umRegistro(AcaoAuditoriaEnum.LOGIN_FALHOU, null))));

        var pagina = auditoriaService.consultar(null, null, null, 0, 50);

        assertThat(pagina.conteudo().getFirst().usuarioId()).isNull();
        assertThat(pagina.conteudo().getFirst().usuarioNome()).isNull();
        verify(usuarioRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("sem datas informadas, consulta do inicio ate agora")
    void deveUsarDatasPadrao() {
        when(registroAuditoriaRepository.findByOcorridoEmBetweenOrderByOcorridoEmDesc(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        auditoriaService.consultar(null, null, null, 0, 50);

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(registroAuditoriaRepository).findByOcorridoEmBetweenOrderByOcorridoEmDesc(
                inicio.capture(), fim.capture(), any(Pageable.class));

        assertThat(inicio.getValue()).isEqualTo(LocalDateTime.of(2000, 1, 1, 0, 0));
        assertThat(fim.getValue()).isEqualTo(LocalDateTime.now(RELOGIO));
    }

    @Test
    @DisplayName("com acao informada, usa a consulta filtrada e o dia inteiro das datas")
    void deveFiltrarPorAcao() {
        when(registroAuditoriaRepository.findByOcorridoEmBetweenAndAcaoOrderByOcorridoEmDesc(
                any(LocalDateTime.class), any(LocalDateTime.class),
                eq(AcaoAuditoriaEnum.EXCLUIDO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        auditoriaService.consultar(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                AcaoAuditoriaEnum.EXCLUIDO, 0, 50);

        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(registroAuditoriaRepository).findByOcorridoEmBetweenAndAcaoOrderByOcorridoEmDesc(
                inicio.capture(), fim.capture(), eq(AcaoAuditoriaEnum.EXCLUIDO), any(Pageable.class));

        assertThat(inicio.getValue()).isEqualTo(LocalDate.of(2026, 9, 1).atStartOfDay());
        assertThat(fim.getValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(fim.getValue().getHour()).isEqualTo(23);

        verify(registroAuditoriaRepository, never())
                .findByOcorridoEmBetweenOrderByOcorridoEmDesc(
                        any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    @DisplayName("CSV deve ter cabecalho e uma linha por registro, com data em formato brasileiro")
    void deveExportarCsv() {
        var usuarioId = UUID.randomUUID();
        when(registroAuditoriaRepository.findByOcorridoEmBetweenOrderByOcorridoEmDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(umRegistro(AcaoAuditoriaEnum.CRIADO, usuarioId)));
        when(usuarioRepository.findAllById(List.of(usuarioId)))
                .thenReturn(List.of(umUsuario(usuarioId, "Maria Souza", "maria@umc.br")));

        String csv = auditoriaService.exportarCsv(null, null, null);
        String[] linhas = csv.split("\n");

        assertThat(linhas).hasSize(2);
        assertThat(linhas[0]).isEqualTo("Quando;Acao;Usuario;E-mail;Recurso;Identificador do recurso");
        assertThat(linhas[1]).startsWith("\"27/09/2026 12:00:00\";\"Criado\";\"Maria Souza\";\"maria@umc.br\"");
    }

    @Test
    @DisplayName("CSV deve escapar aspas e conviver com o separador dentro do dado")
    void deveEscaparOConteudoDoCsv() {
        var usuarioId = UUID.randomUUID();
        when(registroAuditoriaRepository.findByOcorridoEmBetweenOrderByOcorridoEmDesc(
                any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(umRegistro(AcaoAuditoriaEnum.LIDO, usuarioId)));
        when(usuarioRepository.findAllById(List.of(usuarioId)))
                .thenReturn(List.of(umUsuario(usuarioId, "Souza; \"Maria\"", "maria@umc.br")));

        String csv = auditoriaService.exportarCsv(null, null, null);

        assertThat(csv).contains("\"Souza; \"\"Maria\"\"\"");
        assertThat(csv.split("\n")).hasSize(2);
    }

    @Test
    @DisplayName("CSV sem registros traz apenas o cabecalho")
    void deveExportarCsvVazio() {
        when(registroAuditoriaRepository.findByOcorridoEmBetweenAndAcaoOrderByOcorridoEmDesc(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(AcaoAuditoriaEnum.EXCLUIDO)))
                .thenReturn(List.of());

        String csv = auditoriaService.exportarCsv(null, null, AcaoAuditoriaEnum.EXCLUIDO);

        assertThat(csv.strip()).isEqualTo("Quando;Acao;Usuario;E-mail;Recurso;Identificador do recurso");
    }

    @Test
    @DisplayName("descarte apaga pelo prazo de retencao e preserva os registros de anonimizacao")
    void deveDescartarPeloPrazoPreservandoAnonimizacao() {
        when(registroAuditoriaRepository.deleteByOcorridoEmBeforeAndAcaoNot(
                any(LocalDateTime.class), eq(AcaoAuditoriaEnum.ANONIMIZADO)))
                .thenReturn(7L);

        long descartados = auditoriaService.descartarAntigos();

        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(registroAuditoriaRepository).deleteByOcorridoEmBeforeAndAcaoNot(
                limite.capture(), eq(AcaoAuditoriaEnum.ANONIMIZADO));

        assertThat(descartados).isEqualTo(7L);
        assertThat(limite.getValue())
                .isEqualTo(LocalDateTime.now(RELOGIO).minusMonths(RETENCAO_MESES));
    }

    @Test
    @DisplayName("descarte sem nada a apagar devolve zero")
    void deveDescartarNadaQuandoTudoEstaNoPrazo() {
        when(registroAuditoriaRepository.deleteByOcorridoEmBeforeAndAcaoNot(
                any(LocalDateTime.class), eq(AcaoAuditoriaEnum.ANONIMIZADO)))
                .thenReturn(0L);

        assertThat(auditoriaService.descartarAntigos()).isZero();
    }

    @Test
    @DisplayName("deve expor as dez acoes e os quatro recursos como codigo e descricao")
    void deveListarOpcoes() {
        assertThat(auditoriaService.listarAcoes()).hasSize(10);
        assertThat(auditoriaService.listarAcoes().getFirst().codigo()).isEqualTo("CRIADO");
        assertThat(auditoriaService.listarRecursos()).hasSize(4);
        assertThat(auditoriaService.listarRecursos().getFirst().descricao()).isEqualTo("Usuário");
    }

    private RegistroAuditoriaEntity umRegistro(AcaoAuditoriaEnum acao, UUID usuarioId) {
        var registro = new RegistroAuditoriaEntity();
        registro.setId(UUID.randomUUID());
        registro.setOcorridoEm(LocalDateTime.now(RELOGIO));
        registro.setAcao(acao);
        registro.setRecursoTipo(RecursoAuditoriaEnum.USUARIO);
        registro.setRecursoId(UUID.randomUUID());
        registro.setUsuarioId(usuarioId);
        return registro;
    }

    private UsuarioEntity umUsuario(UUID id, String nome, String email) {
        var usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNomeCompleto(nome);
        usuario.setEmail(email);
        return usuario;
    }

    private RegistroAuditoriaEntity capturar() {
        ArgumentCaptor<RegistroAuditoriaEntity> captor =
                ArgumentCaptor.forClass(RegistroAuditoriaEntity.class);
        verify(registroAuditoriaRepository).save(captor.capture());
        return captor.getValue();
    }

    private void autenticarComo(UUID usuarioId) {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(usuarioId.toString())
                .claim("papel", "ALUNO")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }
}
