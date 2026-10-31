package forcamente.api.service;

import forcamente.api.dto.AlterarStatusRequestDTO;
import forcamente.api.dto.RenovarCrefRequestDTO;
import forcamente.api.dto.UsuarioAtualizacaoRequestDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.RecursoAuditoriaEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.LimiteDeTentativasException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.mapper.UsuarioMapper;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.Spy;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UsuarioService - gestao de usuarios pelo administrador")
class UsuarioGestaoServiceTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private IAuditoriaService auditoriaService;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-10-21T13:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private UsuarioService usuarioService;

    private static final UUID ID_ALVO = UUID.randomUUID();
    private static final UUID ID_ADMIN = UUID.randomUUID();

    @Test
    @DisplayName("administrador nao pode inativar a propria conta: sem isso ele se tranca para fora")
    void naoDeveInativarAPropriaConta() {
        assertThatThrownBy(() -> usuarioService.alterarAtivo(ID_ADMIN, false, ID_ADMIN))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("propria conta");

        verify(usuarioRepository, never()).findById(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("administrador PODE reativar a propria conta: a trava e so para trancar")
    void devePermitirReativarAPropriaConta() {
        var eu = umAluno(ID_ADMIN);
        eu.setAtivo(false);
        when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(eu));
        when(usuarioRepository.save(eu)).thenReturn(eu);

        usuarioService.alterarAtivo(ID_ADMIN, true, ID_ADMIN);

        assertThat(eu.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("inativar outra conta funciona e entra na trilha de auditoria")
    void deveInativarOutraConta() {
        var alvo = umAluno(ID_ALVO);
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.save(alvo)).thenReturn(alvo);

        usuarioService.alterarAtivo(ID_ALVO, false, ID_ADMIN);

        assertThat(alvo.isAtivo()).isFalse();
        verify(auditoriaService).registrar(
                forcamente.api.entity.enums.AcaoAuditoriaEnum.ATUALIZADO,
                forcamente.api.entity.enums.RecursoAuditoriaEnum.USUARIO,
                ID_ALVO);
    }

    @Test
    @DisplayName("conta anonimizada nao volta a ficar ativa: a anonimizacao e irreversivel")
    void naoDeveReativarContaAnonimizada() {
        var anonimizada = umAluno(ID_ALVO);
        anonimizada.setAtivo(false);
        anonimizada.setAnonimizadoEm(LocalDateTime.now());
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(anonimizada));

        assertThatThrownBy(() -> usuarioService.alterarAtivo(ID_ALVO, true, ID_ADMIN))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("irreversivel");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("id inexistente vira 404, nao 500")
    void deveRecusarIdInexistente() {
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarAtivo(ID_ALVO, false, ID_ADMIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("manter o proprio e-mail nao acusa conflito: e o que o 'AndIdNot' resolve")
    void naoDeveAcusarConflitoComOProprioEmail() {
        var aluno = umAluno(ID_ALVO);
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.existsByEmailAndIdNot("joao@umc.br", ID_ALVO)).thenReturn(false);
        when(usuarioRepository.save(aluno)).thenReturn(aluno);

        usuarioService.atualizar(ID_ALVO, umaEdicaoDeAluno("Joao da Silva Junior", "joao@umc.br"));

        assertThat(aluno.getNomeCompleto()).isEqualTo("Joao da Silva Junior");
    }

    @Test
    @DisplayName("e-mail de outra conta vira 409")
    void deveRecusarEmailDeOutraConta() {
        var aluno = umAluno(ID_ALVO);
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.existsByEmailAndIdNot("ocupado@umc.br", ID_ALVO)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.atualizar(
                ID_ALVO, umaEdicaoDeAluno("Joao da Silva", "ocupado@umc.br")))
                .isInstanceOf(ConflitoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("conta anonimizada nao pode ser editada")
    void naoDeveEditarContaAnonimizada() {
        var anonimizada = umAluno(ID_ALVO);
        anonimizada.setAnonimizadoEm(LocalDateTime.now());
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(anonimizada));

        assertThatThrownBy(() -> usuarioService.atualizar(
                ID_ALVO, umaEdicaoDeAluno("Nome Novo", "novo@umc.br")))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anonimizada");
    }

    @Test
    @DisplayName("editar aluno nao grava habilitacao nem endereco, mesmo que venham no corpo")
    void naoDeveGravarDadosProfissionaisEmAluno() {
        var aluno = umAluno(ID_ALVO);
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.existsByEmailAndIdNot(any(), any())).thenReturn(false);
        when(usuarioRepository.save(aluno)).thenReturn(aluno);

        var corpoIntrometido = new UsuarioAtualizacaoRequestDTO(
                "Joao da Silva", "joao@umc.br",
                "123456-G/SP", FormacaoEnum.BACHARELADO,
                "08780000", "Rua A", "1", null, "Centro", "Mogi das Cruzes", "SP");

        usuarioService.atualizar(ID_ALVO, corpoIntrometido);

        assertThat(aluno.getCref()).isNull();
        assertThat(aluno.getFormacao()).isNull();
        assertThat(aluno.getCidade()).isNull();
    }

    @Test
    @DisplayName("professor sem CREF ou sem formacao e recusado na edicao")
    void deveExigirHabilitacaoDoProfessor() {
        var professor = umProfessor(ID_ALVO);
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.existsByEmailAndIdNot(any(), any())).thenReturn(false);

        var semCref = new UsuarioAtualizacaoRequestDTO(
                "Maria Souza", "maria@umc.br", null, null,
                "08780000", "Rua B", "2", null, "Centro", "Mogi das Cruzes", "SP");

        assertThatThrownBy(() -> usuarioService.atualizar(ID_ALVO, semCref))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CREF");
    }

    @Test
    @DisplayName("professor com endereco incompleto e recusado na edicao")
    void deveExigirEnderecoDoProfessor() {
        var professor = umProfessor(ID_ALVO);
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.existsByEmailAndIdNot(any(), any())).thenReturn(false);

        var semLogradouro = new UsuarioAtualizacaoRequestDTO(
                "Maria Souza", "maria@umc.br", "123456-G/SP", FormacaoEnum.BACHARELADO,
                "08780000", null, "2", null, "Centro", "Mogi das Cruzes", "SP");

        assertThatThrownBy(() -> usuarioService.atualizar(ID_ALVO, semLogradouro))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("endereco");
    }

    @Test
    @DisplayName("CREF de outro professor vira 409")
    void deveRecusarCrefDeOutroProfessor() {
        var professor = umProfessor(ID_ALVO);
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.existsByEmailAndIdNot(any(), any())).thenReturn(false);
        when(usuarioRepository.existsByCrefAndIdNot("999999-G/SP", ID_ALVO)).thenReturn(true);

        var comCrefAlheio = new UsuarioAtualizacaoRequestDTO(
                "Maria Souza", "maria@umc.br", "999999-G/SP", FormacaoEnum.LICENCIATURA,
                "08780000", "Rua B", "2", null, "Centro", "Mogi das Cruzes", "SP");

        assertThatThrownBy(() -> usuarioService.atualizar(ID_ALVO, comCrefAlheio))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("CREF");
    }

    @Test
    @DisplayName("campo em branco no corpo vira nulo, e nao string vazia no banco")
    void deveTransformarBrancoEmNulo() {
        var edicao = new UsuarioAtualizacaoRequestDTO(
                "  Joao da Silva  ", "  JOAO@UMC.BR ", "  123456-g/sp ", FormacaoEnum.BACHARELADO,
                "08780-000", "Rua A", "1", "   ", "Centro", "Mogi das Cruzes", " sp ");

        assertThat(edicao.nomeCompleto()).isEqualTo("Joao da Silva");
        assertThat(edicao.email()).isEqualTo("joao@umc.br");
        assertThat(edicao.cref()).isEqualTo("123456-G/SP");
        assertThat(edicao.cep()).isEqualTo("08780000");
        assertThat(edicao.complemento()).isNull();
        assertThat(edicao.estado()).isEqualTo("SP");
    }

    @Test
    @DisplayName("a fila de verificacao pede somente professor com CREF pendente")
    void deveListarSomenteProfessorComCrefPendente() {
        usuarioService.listarPendentesDeVerificacao();

        verify(usuarioRepository).findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.PENDENTE);
    }

    @Test
    @DisplayName("aprovar a verificacao grava APROVADO, sem motivo, e registra na trilha")
    void deveAprovarAVerificacaoDoCref() {
        var professor = umProfessor(ID_ALVO);
        professor.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(professor)).thenReturn(professor);

        usuarioService.decidirVerificacaoDoCref(
                ID_ALVO, new AlterarStatusRequestDTO(StatusAprovacaoEnum.APROVADO, null));

        assertThat(professor.getSituacaoCref()).isEqualTo(StatusAprovacaoEnum.APROVADO);
        assertThat(professor.getMotivoRejeicaoCref()).isNull();
        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.APROVADO, RecursoAuditoriaEnum.USUARIO, ID_ALVO);
    }

    @Test
    @DisplayName("rejeitar a verificacao guarda o motivo aparado e registra REJEITADO")
    void deveRejeitarAVerificacaoComOMotivoAparado() {
        var professor = umProfessor(ID_ALVO);
        professor.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(professor)).thenReturn(professor);

        usuarioService.decidirVerificacaoDoCref(ID_ALVO, new AlterarStatusRequestDTO(
                StatusAprovacaoEnum.REJEITADO, "  O CREF informado nao foi encontrado.  "));

        assertThat(professor.getSituacaoCref()).isEqualTo(StatusAprovacaoEnum.REJEITADO);
        assertThat(professor.getMotivoRejeicaoCref())
                .isEqualTo("O CREF informado nao foi encontrado.");
        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.REJEITADO, RecursoAuditoriaEnum.USUARIO, ID_ALVO);
    }

    @Test
    @DisplayName("decidir uma verificacao que nao esta pendente responde 409")
    void deveResponder409AoDecidirVerificacaoQueNaoEstaPendente() {
        var professor = umProfessor(ID_ALVO);
        professor.setSituacaoCref(StatusAprovacaoEnum.APROVADO);

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));

        assertThatThrownBy(() -> usuarioService.decidirVerificacaoDoCref(
                ID_ALVO, new AlterarStatusRequestDTO(StatusAprovacaoEnum.REJEITADO, "tarde")))
                .isInstanceOf(ConflitoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("decidir a verificacao de quem nao e professor responde 400")
    void deveResponder400AoDecidirVerificacaoDeQuemNaoEProfessor() {
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(umAluno(ID_ALVO)));

        assertThatThrownBy(() -> usuarioService.decidirVerificacaoDoCref(
                ID_ALVO, new AlterarStatusRequestDTO(StatusAprovacaoEnum.APROVADO, null)))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("administrador trocando o CREF devolve a verificacao rejeitada para pendente")
    void deveVoltarAVerificacaoRejeitadaParaPendenteAoTrocarOCref() {
        var professor = umProfessor(ID_ALVO);
        professor.setSituacaoCref(StatusAprovacaoEnum.REJEITADO);
        professor.setMotivoRejeicaoCref("CREF nao encontrado");

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(professor)).thenReturn(professor);

        usuarioService.atualizar(ID_ALVO,
                umaEdicaoDeProfessor("654321-G/SP", FormacaoEnum.BACHARELADO));

        assertThat(professor.getSituacaoCref()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(professor.getMotivoRejeicaoCref()).isNull();
    }

    @Test
    @DisplayName("administrador editando sem mexer no CREF nao reabre a verificacao aprovada")
    void naoDeveReabrirAVerificacaoAprovadaQuandoOCrefNaoMuda() {
        var professor = umProfessor(ID_ALVO);
        professor.setSituacaoCref(StatusAprovacaoEnum.APROVADO);

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(professor)).thenReturn(professor);

        usuarioService.atualizar(ID_ALVO,
                umaEdicaoDeProfessor("123456-G/SP", FormacaoEnum.BACHARELADO));

        assertThat(professor.getSituacaoCref()).isEqualTo(StatusAprovacaoEnum.APROVADO);
    }

    @Test
    @DisplayName("renovacao grava o CREF novo e devolve a verificacao para pendente")
    void deveRenovarOCrefEVoltarParaPendente() {
        var professor = umProfessorAprovado();

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(professor)).thenReturn(professor);

        usuarioService.renovarCref(ID_ALVO,
                new RenovarCrefRequestDTO("654321-G/SP", FormacaoEnum.BACHARELADO));

        assertThat(professor.getCref()).isEqualTo("654321-G/SP");
        assertThat(professor.getSituacaoCref()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(professor.getMotivoRejeicaoCref()).isNull();
        verify(auditoriaService).registrar(
                AcaoAuditoriaEnum.ATUALIZADO, RecursoAuditoriaEnum.USUARIO, ID_ALVO);
    }

    @Test
    @DisplayName("renovacao com verificacao pendente responde 400: quem corrige e o administrador")
    void naoDeveRenovarComVerificacaoPendente() {
        var professor = umProfessor(ID_ALVO);
        professor.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));

        assertThatThrownBy(() -> usuarioService.renovarCref(ID_ALVO,
                new RenovarCrefRequestDTO("654321-G/SP", FormacaoEnum.BACHARELADO)))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("com a verificacao rejeitada o professor corrige o proprio CREF, pela decisao de 24/10")
    void deveCorrigirOCrefComVerificacaoRejeitada() {
        var professor = umProfessor(ID_ALVO);
        professor.setSituacaoCref(StatusAprovacaoEnum.REJEITADO);
        professor.setMotivoRejeicaoCref("O numero nao confere com o registro");

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(chamada -> chamada.getArgument(0));

        usuarioService.renovarCref(ID_ALVO,
                new RenovarCrefRequestDTO("654321-G/SP", FormacaoEnum.BACHARELADO));

        assertThat(professor.getCref()).isEqualTo("654321-G/SP");
        assertThat(professor.getSituacaoCref())
                .as("a correcao reabre o pedido na fila do administrador")
                .isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(professor.getMotivoRejeicaoCref())
                .as("o motivo antigo sai, senao a tela mostra recusa de um CREF que ja foi trocado")
                .isNull();
    }

    @Test
    @DisplayName("aluno nao renova CREF, porque nao tem")
    void naoDeveRenovarQuemNaoEProfessor() {
        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(umAluno(ID_ALVO)));

        assertThatThrownBy(() -> usuarioService.renovarCref(ID_ALVO,
                new RenovarCrefRequestDTO("654321-G/SP", FormacaoEnum.BACHARELADO)))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CREF novo que ja e de outro professor responde 409")
    void deveResponder409QuandoOCrefNovoJaEDeOutroProfessor() {
        var professor = umProfessorAprovado();

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.existsByCrefAndIdNot("654321-G/SP", ID_ALVO)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.renovarCref(ID_ALVO,
                new RenovarCrefRequestDTO("654321-G/SP", FormacaoEnum.BACHARELADO)))
                .isInstanceOf(ConflitoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("a quarta renovacao no mesmo dia responde 429")
    void deveBarrarAQuartaRenovacaoDoMesmoDia() {
        var professor = umProfessorAprovado();
        professor.setCrefReenvioDia(LocalDate.of(2026, 10, 21));
        professor.setCrefReenvios(3);

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));

        assertThatThrownBy(() -> usuarioService.renovarCref(ID_ALVO,
                new RenovarCrefRequestDTO("654321-G/SP", FormacaoEnum.BACHARELADO)))
                .isInstanceOf(LimiteDeTentativasException.class)
                .hasMessageContaining("3");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("o contador zera quando o dia vira")
    void deveZerarOContadorQuandoODiaVira() {
        var professor = umProfessorAprovado();
        professor.setCrefReenvioDia(LocalDate.of(2026, 10, 20));
        professor.setCrefReenvios(3);

        when(usuarioRepository.findById(ID_ALVO)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(professor)).thenReturn(professor);

        usuarioService.renovarCref(ID_ALVO,
                new RenovarCrefRequestDTO("654321-G/SP", FormacaoEnum.BACHARELADO));

        assertThat(professor.getCrefReenvioDia()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(professor.getCrefReenvios()).isEqualTo(1);
    }

    private UsuarioEntity umProfessorAprovado() {
        var entity = umProfessor(ID_ALVO);
        entity.setSituacaoCref(StatusAprovacaoEnum.APROVADO);
        entity.setMotivoRejeicaoCref("motivo antigo");
        return entity;
    }

    private UsuarioAtualizacaoRequestDTO umaEdicaoDeProfessor(String cref, FormacaoEnum formacao) {
        return new UsuarioAtualizacaoRequestDTO(
                "Maria Souza", "maria@umc.br", cref, formacao,
                "08780000", "Rua B", "2", null, "Centro", "Mogi das Cruzes", "SP");
    }

    private UsuarioAtualizacaoRequestDTO umaEdicaoDeAluno(String nome, String email) {
        return new UsuarioAtualizacaoRequestDTO(
                nome, email, null, null, null, null, null, null, null, null, null);
    }

    private UsuarioEntity umAluno(UUID id) {
        var entity = new UsuarioEntity();
        entity.setId(id);
        entity.setNomeCompleto("Joao da Silva");
        entity.setEmail("joao@umc.br");
        entity.setSenhaHash("$2a$10$hash");
        entity.setPapel(PapelUsuarioEnum.ALUNO);
        entity.setDataNascimento(LocalDate.of(2000, 1, 1));
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.now());
        return entity;
    }

    private UsuarioEntity umProfessor(UUID id) {
        var entity = umAluno(id);
        entity.setNomeCompleto("Maria Souza");
        entity.setEmail("maria@umc.br");
        entity.setPapel(PapelUsuarioEnum.PROFESSOR);
        entity.setCref("123456-G/SP");
        entity.setFormacao(FormacaoEnum.BACHARELADO);
        entity.setCidade("Mogi das Cruzes");
        entity.setEstado("SP");
        return entity;
    }
}
