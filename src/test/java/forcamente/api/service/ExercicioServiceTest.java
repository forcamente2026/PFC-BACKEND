package forcamente.api.service;

import forcamente.api.dto.ExercicioRequestDTO;
import forcamente.api.dto.ExercicioResponseDTO;
import forcamente.api.entity.ExercicioAprovacaoEntity;
import forcamente.api.entity.ExercicioEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.GrupoMuscularEnum;
import forcamente.api.entity.enums.NivelDificuldadeEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.mapper.ExercicioMapper;
import forcamente.api.repository.IExercicioAprovacaoRepository;
import forcamente.api.repository.IExercicioRepository;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.ExercicioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExercicioService - cadastro, visibilidade por papel e permissoes calculadas")
class ExercicioServiceTest {

    @Mock
    private IExercicioRepository exercicioRepository;

    @Mock
    private IExercicioAprovacaoRepository exercicioAprovacaoRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private ExercicioMapper exercicioMapper;

    @Mock
    private IAuditoriaService auditoriaService;

    @InjectMocks
    private ExercicioService exercicioService;

    private static final UUID ID_AUTOR = UUID.randomUUID();

    private static final UUID ID_OUTRO = UUID.randomUUID();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("exercicio criado por professor nasce PENDENTE, com o autor tirado do token")
    void deveCriarPendenteParaProfessor() {
        autenticarComo(ID_AUTOR, PapelUsuarioEnum.PROFESSOR);

        var requestDTO = umaRequisicaoValida();
        var entity = umaEntidade(null, StatusAprovacaoEnum.APROVADO);

        when(exercicioRepository.existsByNomeIgnoreCase("Supino reto")).thenReturn(false);
        when(exercicioMapper.toEntity(requestDTO)).thenReturn(entity);
        when(exercicioRepository.save(entity)).thenReturn(entity);

        var resultado = exercicioService.criarExercicio(requestDTO);

        assertThat(entity.getStatus())
                .as("professor nao publica sozinho: o item entra na fila")
                .isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(entity.getAutorId()).isEqualTo(ID_AUTOR);
        assertThat(resultado.nome()).isEqualTo("Supino reto");
        assertThat(resultado.aprovacoesNecessarias()).isEqualTo(3);
        assertThat(resultado.aprovacoesDeProfessores()).isZero();

        verify(exercicioRepository).save(entity);
    }

    @Test
    @DisplayName("exercicio criado por administrador nasce APROVADO")
    void deveCriarAprovadoParaAdministrador() {
        autenticarComo(ID_AUTOR, PapelUsuarioEnum.ADMINISTRADOR);

        var requestDTO = umaRequisicaoValida();
        var entity = umaEntidade(null, null);

        when(exercicioRepository.existsByNomeIgnoreCase("Supino reto")).thenReturn(false);
        when(exercicioMapper.toEntity(requestDTO)).thenReturn(entity);
        when(exercicioRepository.save(entity)).thenReturn(entity);

        exercicioService.criarExercicio(requestDTO);

        assertThat(entity.getStatus()).isEqualTo(StatusAprovacaoEnum.APROVADO);
    }

    @Test
    @DisplayName("nao deve cadastrar exercicio com nome ja existente")
    void naoDeveCadastrarExercicioComNomeDuplicado() {
        var requestDTO = umaRequisicaoValida();

        when(exercicioRepository.existsByNomeIgnoreCase("Supino reto")).thenReturn(true);

        assertThatThrownBy(() -> exercicioService.criarExercicio(requestDTO))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Ja existe um exercicio cadastrado");

        verify(exercicioRepository, never()).save(any(ExercicioEntity.class));
    }

    @Test
    @DisplayName("a listagem monta a resposta com os campos calculados pelo back")
    void deveListarComOsCamposCalculados() {
        autenticarComo(ID_AUTOR, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any()))
                .thenReturn(List.of(entity));
        when(usuarioRepository.findAllById(anyCollection()))
                .thenReturn(List.of(umUsuario(ID_AUTOR, "Carlos Pereira")));

        List<ExercicioResponseDTO> resultado = exercicioService.listarExercicios();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().autorNome()).isEqualTo("Carlos Pereira");
        assertThat(resultado.getFirst().status()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(resultado.getFirst().podeEditar())
                .as("o autor edita o proprio item")
                .isTrue();
        assertThat(resultado.getFirst().podeAprovar())
                .as("ninguem aprova o proprio item")
                .isFalse();
    }

    @Test
    @DisplayName("professor que nao e o autor pode aprovar item PENDENTE, e nao pode editar")
    void devePermitirAprovarItemDeOutroProfessor() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any()))
                .thenReturn(List.of(entity));
        when(usuarioRepository.findAllById(anyCollection()))
                .thenReturn(List.of(umUsuario(ID_AUTOR, "Carlos Pereira")));

        var resultado = exercicioService.listarExercicios().getFirst();

        assertThat(resultado.podeAprovar()).isTrue();
        assertThat(resultado.podeEditar()).isFalse();
    }

    @Test
    @DisplayName("professor que ja aprovou nao aprova de novo")
    void naoDevePermitirAprovarDuasVezes() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any()))
                .thenReturn(List.of(entity));
        when(exercicioAprovacaoRepository.findByExercicioIdIn(anyCollection()))
                .thenReturn(List.of(umaAprovacao(entity.getId(), ID_OUTRO)));
        when(usuarioRepository.findAllById(anyCollection()))
                .thenReturn(List.of(umUsuario(ID_AUTOR, "Carlos Pereira"),
                        umUsuario(ID_OUTRO, "Marina Souza")));

        var resultado = exercicioService.listarExercicios().getFirst();

        assertThat(resultado.podeAprovar()).isFalse();
        assertThat(resultado.aprovacoesDeProfessores()).isEqualTo(1);
    }

    @Test
    @DisplayName("exercicio antigo, sem autor, so o administrador edita")
    void deveDeixarSoOAdministradorEditarExercicioSemAutor() {
        var entity = umaEntidade(null, StatusAprovacaoEnum.APROVADO);

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any()))
                .thenReturn(List.of(entity));

        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        assertThat(exercicioService.listarExercicios().getFirst().podeEditar())
                .as("os 15 exercicios da V3 nao tem autor")
                .isFalse();

        autenticarComo(ID_OUTRO, PapelUsuarioEnum.ADMINISTRADOR);

        assertThat(exercicioService.listarExercicios().getFirst().podeEditar()).isTrue();
    }

    @Test
    @DisplayName("aluno buscando exercicio PENDENTE recebe 404, e nao 403: nao se revela o que existe")
    void deveResponder404ParaOQueOAlunoNaoPodeVer() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.ALUNO);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> exercicioService.buscarPorId(entity.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Exercicio nao encontrado");
    }

    @Test
    @DisplayName("o motivo da rejeicao aparece para o autor e some para os outros")
    void deveEsconderOMotivoDeQuemNaoEAutorNemAdministrador() {
        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.REJEITADO);
        entity.setMotivoRejeicao("Sem fonte confiavel.");

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any()))
                .thenReturn(List.of(entity));
        when(usuarioRepository.findAllById(anyCollection()))
                .thenReturn(List.of(umUsuario(ID_AUTOR, "Carlos Pereira")));

        autenticarComo(ID_AUTOR, PapelUsuarioEnum.PROFESSOR);

        assertThat(exercicioService.listarExercicios().getFirst().motivoRejeicao())
                .isEqualTo("Sem fonte confiavel.");

        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        assertThat(exercicioService.listarExercicios().getFirst().motivoRejeicao()).isNull();
    }

    @Test
    @DisplayName("os nomes de quem aprovou saem somente para o administrador")
    void deveMostrarOsAprovadoresSoParaOAdministrador() {
        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any()))
                .thenReturn(List.of(entity));
        when(exercicioAprovacaoRepository.findByExercicioIdIn(anyCollection()))
                .thenReturn(List.of(umaAprovacao(entity.getId(), ID_OUTRO)));
        when(usuarioRepository.findAllById(anyCollection()))
                .thenReturn(List.of(umUsuario(ID_AUTOR, "Carlos Pereira"),
                        umUsuario(ID_OUTRO, "Marina Souza")));

        autenticarComo(ID_AUTOR, PapelUsuarioEnum.ADMINISTRADOR);

        assertThat(exercicioService.listarExercicios().getFirst().professoresQueAprovaram())
                .containsExactly("Marina Souza");

        autenticarComo(ID_AUTOR, PapelUsuarioEnum.PROFESSOR);

        assertThat(exercicioService.listarExercicios().getFirst().professoresQueAprovaram()).isEmpty();
    }

    @Test
    @DisplayName("o filtro por grupo muscular respeita a visibilidade")
    void deveFiltrarPorGrupoMuscularDentroDoQueEVisivel() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.ALUNO);

        var doPeito = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO);
        var dasCostas = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO);
        dasCostas.setNome("Remada curvada");
        dasCostas.setGrupoMuscular(GrupoMuscularEnum.COSTAS);

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any()))
                .thenReturn(List.of(doPeito, dasCostas));
        when(usuarioRepository.findAllById(anyCollection()))
                .thenReturn(List.of(umUsuario(ID_AUTOR, "Carlos Pereira")));

        var resultado = exercicioService.listarPorGrupoMuscular(GrupoMuscularEnum.PEITO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().nome()).isEqualTo("Supino reto");
    }

    @Test
    @DisplayName("deve lancar excecao ao buscar exercicio inexistente")
    void deveLancarExcecaoQuandoExercicioNaoExiste() {
        var idInexistente = UUID.randomUUID();

        when(exercicioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exercicioService.buscarPorId(idInexistente))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Exercicio nao encontrado");
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

    private ExercicioRequestDTO umaRequisicaoValida() {
        return new ExercicioRequestDTO(
                "Supino reto",
                GrupoMuscularEnum.PEITO,
                NivelDificuldadeEnum.INICIANTE,
                "Deite no banco, retraia as escapulas e desca a barra ate o peito.",
                "Abrir demais os cotovelos e quicar a barra no peito.",
                "Rotacao de ombros e 1 serie leve de aquecimento.",
                "Barra e banco reto",
                "https://exemplo.com/supino.gif");
    }

    private ExercicioEntity umaEntidade(UUID autorId, StatusAprovacaoEnum status) {
        var entity = new ExercicioEntity();

        entity.setId(UUID.randomUUID());
        entity.setNome("Supino reto");
        entity.setGrupoMuscular(GrupoMuscularEnum.PEITO);
        entity.setNivel(NivelDificuldadeEnum.INICIANTE);
        entity.setDescricaoExecucao("Deite no banco, retraia as escapulas e desca a barra ate o peito.");
        entity.setAutorId(autorId);
        entity.setStatus(status);
        entity.setCriadoEm(LocalDateTime.now());

        return entity;
    }

    private ExercicioAprovacaoEntity umaAprovacao(UUID exercicioId, UUID professorId) {
        var aprovacao = new ExercicioAprovacaoEntity();

        aprovacao.setId(UUID.randomUUID());
        aprovacao.setExercicioId(exercicioId);
        aprovacao.setProfessorId(professorId);
        aprovacao.setAprovadoEm(LocalDateTime.now());

        return aprovacao;
    }

    private UsuarioEntity umUsuario(UUID id, String nome) {
        var usuario = new UsuarioEntity();

        usuario.setId(id);
        usuario.setNomeCompleto(nome);

        return usuario;
    }

}
