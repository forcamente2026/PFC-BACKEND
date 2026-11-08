package forcamente.api.service;

import forcamente.api.dto.AlterarStatusRequestDTO;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-11-06T13:00:00Z"), ZoneOffset.UTC);

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

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any(), any()))
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

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any(), any()))
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

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any(), any()))
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

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any(), any()))
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

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any(), any()))
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

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any(), any()))
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
    @DisplayName("o filtro por grupo muscular vai para a consulta, junto com a visibilidade")
    void deveFiltrarPorGrupoMuscularNaConsulta() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.ALUNO);

        var doPeito = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO);

        when(exercicioRepository.buscarVisiveis(anyCollection(), any(), any(), any()))
                .thenReturn(List.of(doPeito));
        when(usuarioRepository.findAllById(anyCollection()))
                .thenReturn(List.of(umUsuario(ID_AUTOR, "Carlos Pereira")));

        var resultado = exercicioService.listarPorGrupoMuscular(GrupoMuscularEnum.PEITO);

        assertThat(resultado).hasSize(1);

        verify(exercicioRepository).buscarVisiveis(
                argThat(visiveis -> visiveis.contains(StatusAprovacaoEnum.APROVADO)
                        && !visiveis.contains(StatusAprovacaoEnum.PENDENTE)),
                eq(StatusAprovacaoEnum.REJEITADO),
                any(),
                eq(GrupoMuscularEnum.PEITO));
    }

    @Test
    @DisplayName("professor que nao e o autor recebe 403 ao editar")
    void deveBarrarEdicaoDeQuemNaoEOAutor() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO);

        when(exercicioRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> exercicioService.atualizarExercicio(entity.getId(), umaRequisicaoValida()))
                .isInstanceOf(AccessDeniedException.class);

        verify(exercicioRepository, never()).save(any(ExercicioEntity.class));
    }

    @Test
    @DisplayName("edicao do autor devolve o item para PENDENTE e zera as aprovacoes")
    void deveDevolverParaPendenteNaEdicaoDoAutor() {
        autenticarComo(ID_AUTOR, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
        entity.setMotivoRejeicao("motivo antigo");

        when(exercicioRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(exercicioRepository.save(entity)).thenReturn(entity);

        exercicioService.atualizarExercicio(entity.getId(), umaRequisicaoValida());

        assertThat(entity.getStatus()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(entity.getMotivoRejeicao()).isNull();

        verify(exercicioAprovacaoRepository).deleteByExercicioId(entity.getId());
    }

    @Test
    @DisplayName("edicao do administrador nao reabre a aprovacao")
    void naoDeveReabrirAAprovacaoNaEdicaoDoAdministrador() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.ADMINISTRADOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO);

        when(exercicioRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(exercicioRepository.save(entity)).thenReturn(entity);

        exercicioService.atualizarExercicio(entity.getId(), umaRequisicaoValida());

        assertThat(entity.getStatus()).isEqualTo(StatusAprovacaoEnum.APROVADO);

        verify(exercicioAprovacaoRepository, never()).deleteByExercicioId(any());
    }

    @Test
    @DisplayName("professor que nao e o autor recebe 403 ao excluir")
    void deveBarrarExclusaoDeQuemNaoEOAutor() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO);

        when(exercicioRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> exercicioService.excluirExercicio(entity.getId()))
                .isInstanceOf(AccessDeniedException.class);

        verify(exercicioRepository, never()).delete(any(ExercicioEntity.class));
    }

    @Test
    @DisplayName("o autor nao aprova o proprio exercicio: 403")
    void deveBarrarOAutorAprovandoOProprioItem() {
        autenticarComo(ID_AUTOR, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.buscarParaEscrita(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> exercicioService.aprovarComoProfessor(entity.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("aprovar item que nao esta PENDENTE responde 409, e aprovar duas vezes tambem")
    void deveRecusarAprovacaoRepetidaOuForaDaFila() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        var aprovado = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO);

        when(exercicioRepository.buscarParaEscrita(aprovado.getId())).thenReturn(Optional.of(aprovado));

        assertThatThrownBy(() -> exercicioService.aprovarComoProfessor(aprovado.getId()))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("pendente");

        var pendente = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.buscarParaEscrita(pendente.getId())).thenReturn(Optional.of(pendente));
        when(exercicioAprovacaoRepository.existsByExercicioIdAndProfessorId(pendente.getId(), ID_OUTRO))
                .thenReturn(true);

        assertThatThrownBy(() -> exercicioService.aprovarComoProfessor(pendente.getId()))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("já aprovou");
    }

    @Test
    @DisplayName("a terceira aprovacao de professor publica o exercicio")
    void deveMudarParaAprovadoPelosProfessoresNaTerceira() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.PROFESSOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.PENDENTE);

        when(exercicioRepository.buscarParaEscrita(entity.getId())).thenReturn(Optional.of(entity));
        when(exercicioAprovacaoRepository.countByExercicioId(entity.getId())).thenReturn(3L);

        exercicioService.aprovarComoProfessor(entity.getId());

        assertThat(entity.getStatus()).isEqualTo(StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);
    }

    @Test
    @DisplayName("decidir item ja decidido responde 409")
    void deveRecusarDecidirDuasVezes() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.ADMINISTRADOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.REJEITADO);

        when(exercicioRepository.buscarParaEscrita(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> exercicioService.decidirSituacao(entity.getId(),
                new AlterarStatusRequestDTO(StatusAprovacaoEnum.APROVADO, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("já foi decidido");
    }

    @Test
    @DisplayName("a rejeicao do administrador guarda o motivo aparado")
    void deveGuardarOMotivoDaRejeicao() {
        autenticarComo(ID_OUTRO, PapelUsuarioEnum.ADMINISTRADOR);

        var entity = umaEntidade(ID_AUTOR, StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);

        when(exercicioRepository.buscarParaEscrita(entity.getId())).thenReturn(Optional.of(entity));
        when(exercicioRepository.save(entity)).thenReturn(entity);

        exercicioService.decidirSituacao(entity.getId(),
                new AlterarStatusRequestDTO(StatusAprovacaoEnum.REJEITADO, "  Execucao descrita errada.  "));

        assertThat(entity.getStatus()).isEqualTo(StatusAprovacaoEnum.REJEITADO);
        assertThat(entity.getMotivoRejeicao()).isEqualTo("Execucao descrita errada.");
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
                "https://exemplo.com/supino.gif",
                null);
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
