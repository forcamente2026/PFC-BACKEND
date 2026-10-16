package forcamente.api.service;

import forcamente.api.dto.AlterarStatusArtigoRequestDTO;
import forcamente.api.dto.ArtigoRequestDTO;
import forcamente.api.entity.ArtigoAprovacaoEntity;
import forcamente.api.entity.ArtigoEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.repository.IArtigoAprovacaoRepository;
import forcamente.api.repository.IArtigoRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.ArtigoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtigoService - visibilidade por papel e permissoes calculadas no back")
class ArtigoServiceTest {

    private static final UUID ARTIGO = UUID.randomUUID();
    private static final UUID AUTOR = UUID.randomUUID();
    private static final UUID OUTRO_PROFESSOR = UUID.randomUUID();
    private static final UUID TERCEIRO_PROFESSOR = UUID.randomUUID();
    private static final UUID ALUNO = UUID.randomUUID();
    private static final UUID ADMINISTRADOR = UUID.randomUUID();

    private static final String FONTE =
            "https://doi.org/10.1519/JSC.0000000000004526";

    @Mock
    private IArtigoRepository artigoRepository;

    @Mock
    private IArtigoAprovacaoRepository artigoAprovacaoRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    private ArtigoService artigoService;

    private static final Clock RELOGIO =
            Clock.fixed(Instant.parse("2026-10-15T13:00:00Z"), ZoneOffset.UTC);
    @Mock
    private IAuditoriaService auditoriaService;


    @BeforeEach
    void configurar() {
        artigoService = new ArtigoService(
                artigoRepository, artigoAprovacaoRepository, usuarioRepository,
                auditoriaService, RELOGIO);
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("o aluno so pede os artigos que estao no site")
    void deveLimitarOAlunoAosArtigosPublicados() {
        autenticarComo(ALUNO, PapelUsuarioEnum.ALUNO);

        artigoService.listarArtigos(null);

        verify(artigoRepository).buscarVisiveis(
                Set.of(StatusAprovacaoEnum.APROVADO,
                        StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES),
                StatusAprovacaoEnum.REJEITADO,
                ALUNO,
                null);
    }

    @Test
    @DisplayName("o professor pede tambem os pendentes, de todos, para poder aprovar")
    void deveDeixarOProfessorVerOsPendentesDeTodos() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        artigoService.listarArtigos(null);

        verify(artigoRepository).buscarVisiveis(
                Set.of(StatusAprovacaoEnum.APROVADO,
                        StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES,
                        StatusAprovacaoEnum.PENDENTE),
                StatusAprovacaoEnum.REJEITADO,
                OUTRO_PROFESSOR,
                null);
    }

    @Test
    @DisplayName("o administrador pede as quatro situacoes")
    void deveDeixarOAdministradorVerTodasAsSituacoes() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        artigoService.listarArtigos(null);

        verify(artigoRepository).buscarVisiveis(
                Set.of(StatusAprovacaoEnum.PENDENTE,
                        StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES,
                        StatusAprovacaoEnum.APROVADO,
                        StatusAprovacaoEnum.REJEITADO),
                StatusAprovacaoEnum.REJEITADO,
                ADMINISTRADOR,
                null);
    }

    @Test
    @DisplayName("o filtro de situacao da rota chega ao repositorio")
    void deveRepassarOFiltroDeSituacao() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        artigoService.listarArtigos(StatusAprovacaoEnum.PENDENTE);

        verify(artigoRepository).buscarVisiveis(
                any(), any(), any(), eq(StatusAprovacaoEnum.PENDENTE));
    }

    @Test
    @DisplayName("o aluno recebe 404, e nao 403, ao buscar um artigo pendente")
    void deveResponder404QuandoOAlunoBuscaUmArtigoPendente() {
        autenticarComo(ALUNO, PapelUsuarioEnum.ALUNO);

        when(artigoRepository.findById(ARTIGO))
                .thenReturn(Optional.of(artigo(StatusAprovacaoEnum.PENDENTE, AUTOR)));

        assertThatThrownBy(() -> artigoService.buscarPorId(ARTIGO))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("o autor ve o proprio rejeitado, com o motivo, e pode editar")
    void deveDeixarOAutorVerOProprioRejeitadoComOMotivo() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        var rejeitado = artigo(StatusAprovacaoEnum.REJEITADO, AUTOR);
        rejeitado.setMotivoRejeicao("A fonte nao e um estudo revisado por pares.");

        prepararBuscaPorId(rejeitado, List.of(), List.of(usuario(AUTOR, "Prof. Autor")));

        var resposta = artigoService.buscarPorId(ARTIGO);

        assertThat(resposta.motivoRejeicao())
                .isEqualTo("A fonte nao e um estudo revisado por pares.");
        assertThat(resposta.podeEditar()).isTrue();
        assertThat(resposta.podeAprovar()).isFalse();
        assertThat(resposta.autorNome()).isEqualTo("Prof. Autor");
    }

    @Test
    @DisplayName("outro professor pode aprovar um pendente, mas nao editar")
    void devePermitirQueOutroProfessorAproveUmPendente() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        prepararBuscaPorId(
                artigo(StatusAprovacaoEnum.PENDENTE, AUTOR),
                List.of(),
                List.of(usuario(AUTOR, "Prof. Autor")));

        var resposta = artigoService.buscarPorId(ARTIGO);

        assertThat(resposta.podeAprovar()).isTrue();
        assertThat(resposta.podeEditar()).isFalse();
        assertThat(resposta.aprovacoesDeProfessores()).isZero();
    }

    @Test
    @DisplayName("o autor nao aprova o proprio artigo")
    void naoDevePermitirQueOAutorAproveOProprioArtigo() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        prepararBuscaPorId(
                artigo(StatusAprovacaoEnum.PENDENTE, AUTOR),
                List.of(),
                List.of(usuario(AUTOR, "Prof. Autor")));

        var resposta = artigoService.buscarPorId(ARTIGO);

        assertThat(resposta.podeAprovar()).isFalse();
        assertThat(resposta.podeEditar()).isTrue();
    }

    @Test
    @DisplayName("o mesmo professor nao aprova duas vezes")
    void naoDevePermitirQueOMesmoProfessorAproveDuasVezes() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        prepararBuscaPorId(
                artigo(StatusAprovacaoEnum.PENDENTE, AUTOR),
                List.of(aprovacao(OUTRO_PROFESSOR)),
                List.of(usuario(AUTOR, "Prof. Autor"),
                        usuario(OUTRO_PROFESSOR, "Prof. Outro")));

        var resposta = artigoService.buscarPorId(ARTIGO);

        assertThat(resposta.podeAprovar()).isFalse();
        assertThat(resposta.aprovacoesDeProfessores()).isEqualTo(1);
    }

    @Test
    @DisplayName("o aluno nao edita nem aprova nada")
    void naoDeveDeixarOAlunoEditarNemAprovar() {
        autenticarComo(ALUNO, PapelUsuarioEnum.ALUNO);

        prepararBuscaPorId(
                artigo(StatusAprovacaoEnum.APROVADO, AUTOR),
                List.of(),
                List.of(usuario(AUTOR, "Prof. Autor")));

        var resposta = artigoService.buscarPorId(ARTIGO);

        assertThat(resposta.podeEditar()).isFalse();
        assertThat(resposta.podeAprovar()).isFalse();
        assertThat(resposta.motivoRejeicao()).isNull();
    }

    @Test
    @DisplayName("o administrador edita qualquer um e ve quem aprovou")
    void deveDarEdicaoAoAdministradorEOsNomesDeQuemAprovou() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        prepararBuscaPorId(
                artigo(StatusAprovacaoEnum.PENDENTE, AUTOR),
                List.of(aprovacao(OUTRO_PROFESSOR), aprovacao(TERCEIRO_PROFESSOR)),
                List.of(usuario(AUTOR, "Prof. Autor"),
                        usuario(OUTRO_PROFESSOR, "Prof. Outro"),
                        usuario(TERCEIRO_PROFESSOR, "Prof. Terceiro")));

        var resposta = artigoService.buscarPorId(ARTIGO);

        assertThat(resposta.podeEditar()).isTrue();
        assertThat(resposta.podeAprovar()).isFalse();
        assertThat(resposta.aprovacoesDeProfessores()).isEqualTo(2);
        assertThat(resposta.professoresQueAprovaram())
                .containsExactlyInAnyOrder("Prof. Outro", "Prof. Terceiro");
    }

    @Test
    @DisplayName("o professor ve a contagem mas nao os nomes de quem aprovou")
    void naoDeveExporOsNomesDeQuemAprovouParaOProfessor() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        prepararBuscaPorId(
                artigo(StatusAprovacaoEnum.PENDENTE, AUTOR),
                List.of(aprovacao(TERCEIRO_PROFESSOR)),
                List.of(usuario(AUTOR, "Prof. Autor"),
                        usuario(TERCEIRO_PROFESSOR, "Prof. Terceiro")));

        var resposta = artigoService.buscarPorId(ARTIGO);

        assertThat(resposta.aprovacoesDeProfessores()).isEqualTo(1);
        assertThat(resposta.professoresQueAprovaram()).isEmpty();
    }

    @Test
    @DisplayName("artigo do professor nasce pendente, com autor e data do relogio")
    void deveNascerPendenteQuandoOProfessorCria() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);
        prepararCriacao();

        artigoService.criarArtigo(requisicao(FONTE, 2024));

        var salvo = capturarSalvo();
        assertThat(salvo.getStatus()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(salvo.getAutorId()).isEqualTo(AUTOR);
        assertThat(salvo.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 10, 15, 13, 0));
    }

    @Test
    @DisplayName("artigo do administrador nasce aprovado, sem passar pela fila")
    void deveNascerAprovadoQuandoOAdministradorCria() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);
        prepararCriacao();

        artigoService.criarArtigo(requisicao(FONTE, 2024));

        assertThat(capturarSalvo().getStatus()).isEqualTo(StatusAprovacaoEnum.APROVADO);
    }

    @Test
    @DisplayName("fonte repetida responde 409 e nao grava")
    void deveResponder409QuandoAFonteJaExiste() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        when(artigoRepository.existsByFonte(FONTE)).thenReturn(true);

        assertThatThrownBy(() -> artigoService.criarArtigo(requisicao(FONTE, 2024)))
                .isInstanceOf(ConflitoException.class);

        verify(artigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("o back apara o espaco da fonte antes de checar e de gravar")
    void deveApararOEspacoDaFonteAntesDeChecarEGravar() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);
        prepararCriacao();

        artigoService.criarArtigo(requisicao("  " + FONTE + "  ", 2024));

        verify(artigoRepository).existsByFonte(FONTE);
        assertThat(capturarSalvo().getFonte()).isEqualTo(FONTE);
    }

    @Test
    @DisplayName("ano no futuro responde 400, com o ano vigente do relogio")
    void deveRecusarAnoNoFuturo() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        assertThatThrownBy(() -> artigoService.criarArtigo(requisicao(FONTE, 2027)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("2026");

        verify(artigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("a trilha recebe CRIADO com recurso ARTIGO")
    void deveRegistrarNaTrilhaAoCriar() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);
        prepararCriacao();

        artigoService.criarArtigo(requisicao(FONTE, 2024));

        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.CRIADO, RecursoAuditoriaEnum.ARTIGO, ARTIGO);
    }

    @Test
    @DisplayName("professor que nao indicou o artigo recebe 403 ao editar")
    void deveResponder403QuandoOutroProfessorTentaEditar() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        when(artigoRepository.findById(ARTIGO))
                .thenReturn(Optional.of(artigo(StatusAprovacaoEnum.PENDENTE, AUTOR)));

        assertThatThrownBy(() -> artigoService.atualizarArtigo(ARTIGO, requisicao(FONTE, 2024)))
                .isInstanceOf(AccessDeniedException.class);

        verify(artigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("professor que nao indicou o artigo recebe 403 ao excluir")
    void deveResponder403QuandoOutroProfessorTentaExcluir() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        when(artigoRepository.findById(ARTIGO))
                .thenReturn(Optional.of(artigo(StatusAprovacaoEnum.APROVADO, AUTOR)));

        assertThatThrownBy(() -> artigoService.excluirArtigo(ARTIGO))
                .isInstanceOf(AccessDeniedException.class);

        verify(artigoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("edicao do autor volta para pendente, apaga o motivo e zera as aprovacoes")
    void deveVoltarParaPendenteEZerarAsAprovacoesQuandoOAutorEdita() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        var rejeitado = artigo(StatusAprovacaoEnum.REJEITADO, AUTOR);
        rejeitado.setMotivoRejeicao("A fonte nao e um estudo revisado por pares.");

        prepararAtualizacao(rejeitado);

        artigoService.atualizarArtigo(ARTIGO, requisicao(FONTE, 2024));

        var salvo = capturarSalvo();
        assertThat(salvo.getStatus()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(salvo.getMotivoRejeicao()).isNull();
        verify(artigoAprovacaoRepository).deleteByArtigoId(ARTIGO);
    }

    @Test
    @DisplayName("edicao do administrador nao muda a situacao nem zera as aprovacoes")
    void naoDeveMudarASituacaoQuandoOAdministradorEdita() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        prepararAtualizacao(artigo(StatusAprovacaoEnum.APROVADO, AUTOR));

        artigoService.atualizarArtigo(ARTIGO, requisicao(FONTE, 2024));

        assertThat(capturarSalvo().getStatus()).isEqualTo(StatusAprovacaoEnum.APROVADO);
        verify(artigoAprovacaoRepository, never()).deleteByArtigoId(any());
    }

    @Test
    @DisplayName("editar artigo inexistente responde 404")
    void deveResponder404AoEditarUmArtigoInexistente() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        when(artigoRepository.findById(ARTIGO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artigoService.atualizarArtigo(ARTIGO, requisicao(FONTE, 2024)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("a trilha recebe EXCLUIDO e o artigo sai do banco")
    void deveRegistrarNaTrilhaAoExcluir() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        var artigo = artigo(StatusAprovacaoEnum.PENDENTE, AUTOR);
        when(artigoRepository.findById(ARTIGO)).thenReturn(Optional.of(artigo));

        artigoService.excluirArtigo(ARTIGO);

        verify(artigoRepository).delete(artigo);
        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.EXCLUIDO, RecursoAuditoriaEnum.ARTIGO, ARTIGO);
    }

    @Test
    @DisplayName("a primeira aprovacao e registrada e o artigo continua pendente")
    void deveContarAAprovacaoDeOutroProfessorSemPublicar() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);
        prepararAprovacao(artigo(StatusAprovacaoEnum.PENDENTE, AUTOR), 1L);

        var resposta = artigoService.aprovarComoProfessor(ARTIGO);

        var aprovacao = capturarAprovacao();
        assertThat(aprovacao.getProfessorId()).isEqualTo(OUTRO_PROFESSOR);
        assertThat(aprovacao.getAprovadoEm()).isEqualTo(LocalDateTime.of(2026, 10, 15, 13, 0));
        assertThat(resposta.status()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        verify(artigoRepository, never()).save(any());
        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.APROVADO, RecursoAuditoriaEnum.ARTIGO, ARTIGO);
    }

    @Test
    @DisplayName("na terceira aprovacao de professor o artigo entra no site")
    void devePublicarNaTerceiraAprovacaoDeProfessor() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);
        prepararAprovacao(artigo(StatusAprovacaoEnum.PENDENTE, AUTOR), 3L);

        var resposta = artigoService.aprovarComoProfessor(ARTIGO);

        assertThat(resposta.status())
                .isEqualTo(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
        verify(artigoRepository).save(any());
    }

    @Test
    @DisplayName("quem indicou o artigo recebe 403 ao tentar aprovar o proprio")
    void deveResponder403QuandoOAutorTentaAprovarOProprio() {
        autenticarComo(AUTOR, PapelUsuarioEnum.PROFESSOR);

        when(artigoRepository.findById(ARTIGO))
                .thenReturn(Optional.of(artigo(StatusAprovacaoEnum.PENDENTE, AUTOR)));

        assertThatThrownBy(() -> artigoService.aprovarComoProfessor(ARTIGO))
                .isInstanceOf(AccessDeniedException.class);

        verify(artigoAprovacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("aprovar artigo que nao esta pendente responde 409")
    void deveResponder409AoAprovarUmArtigoQueNaoEstaPendente() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        when(artigoRepository.findById(ARTIGO))
                .thenReturn(Optional.of(artigo(StatusAprovacaoEnum.APROVADO, AUTOR)));

        assertThatThrownBy(() -> artigoService.aprovarComoProfessor(ARTIGO))
                .isInstanceOf(ConflitoException.class);

        verify(artigoAprovacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("o mesmo professor aprovando de novo responde 409")
    void deveResponder409QuandoOProfessorAprovaDuasVezes() {
        autenticarComo(OUTRO_PROFESSOR, PapelUsuarioEnum.PROFESSOR);

        when(artigoRepository.findById(ARTIGO))
                .thenReturn(Optional.of(artigo(StatusAprovacaoEnum.PENDENTE, AUTOR)));
        when(artigoAprovacaoRepository.existsByArtigoIdAndProfessorId(ARTIGO, OUTRO_PROFESSOR))
                .thenReturn(true);

        assertThatThrownBy(() -> artigoService.aprovarComoProfessor(ARTIGO))
                .isInstanceOf(ConflitoException.class);

        verify(artigoAprovacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("a fila do administrador pede pendentes e publicados pelos professores")
    void deveListarAFilaDoAdministradorComAsDuasSituacoes() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        artigoService.listarPendentes();

        verify(artigoRepository).findByStatusInOrderByCriadoEmAsc(
                Set.of(StatusAprovacaoEnum.PENDENTE,
                        StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES));
    }

    @Test
    @DisplayName("o administrador aprova, o motivo e apagado e a trilha recebe APROVADO")
    void deveAprovarComoAdministradorEApagarOMotivo() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        var pendente = artigo(StatusAprovacaoEnum.PENDENTE, AUTOR);
        pendente.setMotivoRejeicao("motivo antigo");

        prepararDecisao(pendente);

        artigoService.decidirSituacao(ARTIGO,
                new AlterarStatusArtigoRequestDTO(StatusAprovacaoEnum.APROVADO, null));

        var salvo = capturarSalvo();
        assertThat(salvo.getStatus()).isEqualTo(StatusAprovacaoEnum.APROVADO);
        assertThat(salvo.getMotivoRejeicao()).isNull();
        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.APROVADO, RecursoAuditoriaEnum.ARTIGO, ARTIGO);
    }

    @Test
    @DisplayName("o administrador rejeita, o motivo e guardado aparado e a trilha recebe REJEITADO")
    void deveRejeitarGuardandoOMotivoAparado() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        prepararDecisao(artigo(StatusAprovacaoEnum.PENDENTE, AUTOR));

        artigoService.decidirSituacao(ARTIGO, new AlterarStatusArtigoRequestDTO(
                StatusAprovacaoEnum.REJEITADO, "  A fonte nao e revisada por pares.  "));

        var salvo = capturarSalvo();
        assertThat(salvo.getStatus()).isEqualTo(StatusAprovacaoEnum.REJEITADO);
        assertThat(salvo.getMotivoRejeicao()).isEqualTo("A fonte nao e revisada por pares.");
        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.REJEITADO, RecursoAuditoriaEnum.ARTIGO, ARTIGO);
    }

    @Test
    @DisplayName("o administrador decide um artigo que os professores publicaram")
    void devePermitirDecidirUmArtigoPublicadoPelosProfessores() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        prepararDecisao(artigo(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES, AUTOR));

        artigoService.decidirSituacao(ARTIGO, new AlterarStatusArtigoRequestDTO(
                StatusAprovacaoEnum.REJEITADO, "Retratado pela revista."));

        assertThat(capturarSalvo().getStatus()).isEqualTo(StatusAprovacaoEnum.REJEITADO);
    }

    @Test
    @DisplayName("decidir um artigo ja decidido responde 409")
    void deveResponder409AoDecidirUmArtigoJaDecidido() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        when(artigoRepository.findById(ARTIGO))
                .thenReturn(Optional.of(artigo(StatusAprovacaoEnum.REJEITADO, AUTOR)));

        assertThatThrownBy(() -> artigoService.decidirSituacao(ARTIGO,
                new AlterarStatusArtigoRequestDTO(StatusAprovacaoEnum.APROVADO, null)))
                .isInstanceOf(ConflitoException.class);

        verify(artigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("decidir artigo inexistente responde 404")
    void deveResponder404AoDecidirUmArtigoInexistente() {
        autenticarComo(ADMINISTRADOR, PapelUsuarioEnum.ADMINISTRADOR);

        when(artigoRepository.findById(ARTIGO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artigoService.decidirSituacao(ARTIGO,
                new AlterarStatusArtigoRequestDTO(StatusAprovacaoEnum.APROVADO, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private void prepararAprovacao(ArtigoEntity existente, long aprovacoesDepois) {
        when(artigoRepository.findById(ARTIGO)).thenReturn(Optional.of(existente));
        when(artigoAprovacaoRepository.countByArtigoId(ARTIGO)).thenReturn(aprovacoesDepois);
        when(artigoAprovacaoRepository.findByArtigoIdIn(any())).thenReturn(List.of());
        when(usuarioRepository.findAllById(any()))
                .thenReturn(List.of(usuario(AUTOR, "Prof. Autor")));
    }

    private void prepararDecisao(ArtigoEntity existente) {
        when(artigoRepository.findById(ARTIGO)).thenReturn(Optional.of(existente));
        when(artigoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(artigoAprovacaoRepository.findByArtigoIdIn(any())).thenReturn(List.of());
        when(usuarioRepository.findAllById(any()))
                .thenReturn(List.of(usuario(AUTOR, "Prof. Autor")));
    }

    private ArtigoAprovacaoEntity capturarAprovacao() {
        var captor = ArgumentCaptor.forClass(ArtigoAprovacaoEntity.class);
        verify(artigoAprovacaoRepository).save(captor.capture());
        return captor.getValue();
    }

    private void prepararCriacao() {
        when(artigoRepository.save(any())).thenAnswer(invocacao -> {
            ArtigoEntity salvo = invocacao.getArgument(0);
            salvo.setId(ARTIGO);
            return salvo;
        });
        when(artigoAprovacaoRepository.findByArtigoIdIn(any())).thenReturn(List.of());
        when(usuarioRepository.findAllById(any()))
                .thenReturn(List.of(usuario(AUTOR, "Prof. Autor")));
    }

    private void prepararAtualizacao(ArtigoEntity existente) {
        when(artigoRepository.findById(ARTIGO)).thenReturn(Optional.of(existente));
        when(artigoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(artigoAprovacaoRepository.findByArtigoIdIn(any())).thenReturn(List.of());
        when(usuarioRepository.findAllById(any()))
                .thenReturn(List.of(usuario(AUTOR, "Prof. Autor")));
    }

    private ArtigoEntity capturarSalvo() {
        var captor = ArgumentCaptor.forClass(ArtigoEntity.class);
        verify(artigoRepository).save(captor.capture());
        return captor.getValue();
    }

    private ArtigoRequestDTO requisicao(String fonte, Integer ano) {
        return new ArtigoRequestDTO(
                "Tensao mecanica como principal motor da hipertrofia",
                CategoriaArtigoEnum.FISIOLOGIA,
                "Revisao dos mecanismos de sinalizacao mTOR.",
                fonte,
                "Schoenfeld BJ",
                "Rev. sistematica",
                ano,
                12);
    }

    private void prepararBuscaPorId(ArtigoEntity artigo,
                                    List<ArtigoAprovacaoEntity> aprovacoes,
                                    List<UsuarioEntity> usuarios) {

        when(artigoRepository.findById(ARTIGO)).thenReturn(Optional.of(artigo));
        when(artigoAprovacaoRepository.findByArtigoIdIn(any())).thenReturn(aprovacoes);
        when(usuarioRepository.findAllById(any())).thenReturn(usuarios);
    }

    private ArtigoEntity artigo(StatusAprovacaoEnum status, UUID autorId) {
        var artigo = new ArtigoEntity();
        artigo.setId(ARTIGO);
        artigo.setTitulo("Tensao mecanica como principal motor da hipertrofia");
        artigo.setCategoria(CategoriaArtigoEnum.FISIOLOGIA);
        artigo.setResumo("Revisao dos mecanismos de sinalizacao mTOR.");
        artigo.setFonte("https://doi.org/10.1519/JSC.0000000000004526");
        artigo.setStatus(status);
        artigo.setAutorId(autorId);
        artigo.setCriadoEm(LocalDateTime.of(2026, 10, 15, 10, 0));
        return artigo;
    }

    private ArtigoAprovacaoEntity aprovacao(UUID professorId) {
        var aprovacao = new ArtigoAprovacaoEntity();
        aprovacao.setId(UUID.randomUUID());
        aprovacao.setArtigoId(ARTIGO);
        aprovacao.setProfessorId(professorId);
        aprovacao.setAprovadoEm(LocalDateTime.of(2026, 10, 15, 11, 0));
        return aprovacao;
    }

    private UsuarioEntity usuario(UUID id, String nomeCompleto) {
        var usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setNomeCompleto(nomeCompleto);
        return usuario;
    }

    private void autenticarComo(UUID usuarioId, PapelUsuarioEnum papel) {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(usuarioId.toString())
                .claim("papel", papel.name())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null, List.of()));
    }
}