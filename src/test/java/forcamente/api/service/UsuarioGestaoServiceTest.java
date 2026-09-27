package forcamente.api.service;

import forcamente.api.dto.UsuarioAtualizacaoRequestDTO;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.exception.ConflitoException;
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
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
