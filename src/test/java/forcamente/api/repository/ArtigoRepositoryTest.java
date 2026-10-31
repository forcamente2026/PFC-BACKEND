package forcamente.api.repository;

import forcamente.api.entity.ArtigoAprovacaoEntity;
import forcamente.api.entity.ArtigoEntity;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.CategoriaArtigoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Artigos no banco - as consultas executadas contra o PostgreSQL")
class ArtigoRepositoryTest {

    private static final Set<StatusAprovacaoEnum> VISIVEIS_PARA_ALUNO = Set.of(
            StatusAprovacaoEnum.APROVADO,
            StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES);

    private static final Set<StatusAprovacaoEnum> VISIVEIS_PARA_PROFESSOR = Set.of(
            StatusAprovacaoEnum.APROVADO,
            StatusAprovacaoEnum.APROVADO_PELOS_PROFESSORES,
            StatusAprovacaoEnum.PENDENTE);

    @Autowired
    private IArtigoRepository artigoRepository;

    @Autowired
    private IArtigoAprovacaoRepository aprovacaoRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Test
    @DisplayName("a JPQL da visibilidade executa: o parametro nomeado da consulta casa com o do @Param")
    void deveExecutarAJpqlDaVisibilidade() {
        var autorId = professores().getFirst();

        assertThatCode(() -> artigoRepository.buscarVisiveis(
                VISIVEIS_PARA_ALUNO, StatusAprovacaoEnum.REJEITADO, autorId, null))
                .as("nome de parametro divergente do @Param nao quebra a compilacao, so a execucao")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o aluno nao ve pendente nem rejeitado de outra pessoa")
    void deveEsconderPendenteERejeitadoDeOutroAutor() {
        var autor = professores().getFirst();

        var aprovado = artigoSalvo(StatusAprovacaoEnum.APROVADO, autor, umaHoraAtras());
        var pendente = artigoSalvo(StatusAprovacaoEnum.PENDENTE, autor, umaHoraAtras());
        var rejeitado = artigoSalvo(StatusAprovacaoEnum.REJEITADO, autor, umaHoraAtras());

        var visiveis = idsDe(artigoRepository.buscarVisiveis(
                VISIVEIS_PARA_ALUNO, StatusAprovacaoEnum.REJEITADO, UUID.randomUUID(), null));

        assertThat(visiveis).contains(aprovado.getId());
        assertThat(visiveis).doesNotContain(pendente.getId(), rejeitado.getId());
    }

    @Test
    @DisplayName("o autor ve o proprio artigo rejeitado, e o de outro autor continua escondido")
    void deveMostrarORejeitadoApenasParaOSeuAutor() {
        var professores = professores();
        var autor = professores.get(0);
        var outroAutor = professores.get(1);

        var meuRejeitado = artigoSalvo(StatusAprovacaoEnum.REJEITADO, autor, umaHoraAtras());
        var rejeitadoDeOutro = artigoSalvo(StatusAprovacaoEnum.REJEITADO, outroAutor, umaHoraAtras());

        var visiveis = idsDe(artigoRepository.buscarVisiveis(
                VISIVEIS_PARA_ALUNO, StatusAprovacaoEnum.REJEITADO, autor, null));

        assertThat(visiveis).contains(meuRejeitado.getId());
        assertThat(visiveis).doesNotContain(rejeitadoDeOutro.getId());
    }

    @Test
    @DisplayName("o filtro de status corta tambem o rejeitado do proprio autor")
    void deveFiltrarPorStatusQuandoInformado() {
        var autor = professores().getFirst();

        var pendente = artigoSalvo(StatusAprovacaoEnum.PENDENTE, autor, umaHoraAtras());
        var aprovado = artigoSalvo(StatusAprovacaoEnum.APROVADO, autor, umaHoraAtras());
        var rejeitado = artigoSalvo(StatusAprovacaoEnum.REJEITADO, autor, umaHoraAtras());

        var apenasPendentes = idsDe(artigoRepository.buscarVisiveis(
                VISIVEIS_PARA_PROFESSOR, StatusAprovacaoEnum.REJEITADO, autor,
                StatusAprovacaoEnum.PENDENTE));

        assertThat(apenasPendentes).contains(pendente.getId());
        assertThat(apenasPendentes)
                .as("o filtro de status e aplicado em cima da visibilidade, nao ao lado dela")
                .doesNotContain(aprovado.getId(), rejeitado.getId());
    }

    @Test
    @DisplayName("a listagem sai do mais novo para o mais antigo")
    void deveOrdenarDoMaisNovoParaOMaisAntigo() {
        var autor = professores().getFirst();

        var antigo = artigoSalvo(StatusAprovacaoEnum.APROVADO, autor, LocalDateTime.now().minusDays(3));
        var novo = artigoSalvo(StatusAprovacaoEnum.APROVADO, autor, LocalDateTime.now().minusMinutes(1));

        var nossos = idsDe(artigoRepository.buscarVisiveis(
                VISIVEIS_PARA_ALUNO, StatusAprovacaoEnum.REJEITADO, autor, null))
                .stream()
                .filter(id -> id.equals(antigo.getId()) || id.equals(novo.getId()))
                .toList();

        assertThat(nossos).containsExactly(novo.getId(), antigo.getId());
    }

    @Test
    @DisplayName("a fila de pendentes sai do mais antigo para o mais novo")
    void deveListarPendentesDoMaisAntigoParaOMaisNovo() {
        var autor = professores().getFirst();

        var antigo = artigoSalvo(StatusAprovacaoEnum.PENDENTE, autor, LocalDateTime.now().minusDays(3));
        var novo = artigoSalvo(StatusAprovacaoEnum.PENDENTE, autor, LocalDateTime.now().minusMinutes(1));

        var nossos = idsDe(artigoRepository.findByStatusInOrderByCriadoEmAsc(
                Set.of(StatusAprovacaoEnum.PENDENTE)))
                .stream()
                .filter(id -> id.equals(antigo.getId()) || id.equals(novo.getId()))
                .toList();

        assertThat(nossos).containsExactly(antigo.getId(), novo.getId());
    }

    @Test
    @DisplayName("a fonte repetida e detectada, e o proprio artigo nao conta como repeticao")
    void deveDetectarFonteRepetidaIgnorandoOProprioArtigo() {
        var autor = professores().getFirst();
        var artigo = artigoSalvo(StatusAprovacaoEnum.APROVADO, autor, umaHoraAtras());

        assertThat(artigoRepository.existsByFonte(artigo.getFonte())).isTrue();
        assertThat(artigoRepository.existsByFonteAndIdNot(artigo.getFonte(), artigo.getId()))
                .as("na edicao, a propria fonte do artigo nao pode ser lida como duplicada")
                .isFalse();
    }

    @Test
    @DisplayName("as aprovacoes do artigo sao contadas, encontradas e apagadas")
    void deveContarEncontrarEApagarAsAprovacoes() {
        var professores = professores();
        var autor = professores.get(0);
        var professorQueAprova = professores.get(1);

        var artigo = artigoSalvo(StatusAprovacaoEnum.PENDENTE, autor, umaHoraAtras());

        var aprovacao = new ArtigoAprovacaoEntity();
        aprovacao.setArtigoId(artigo.getId());
        aprovacao.setProfessorId(professorQueAprova);
        aprovacao.setAprovadoEm(LocalDateTime.now());
        aprovacaoRepository.saveAndFlush(aprovacao);

        assertThat(aprovacaoRepository.countByArtigoId(artigo.getId())).isEqualTo(1);
        assertThat(aprovacaoRepository.existsByArtigoIdAndProfessorId(artigo.getId(), professorQueAprova))
                .isTrue();
        assertThat(aprovacaoRepository.existsByArtigoIdAndProfessorId(artigo.getId(), autor))
                .as("quem nao aprovou nao pode aparecer como aprovador")
                .isFalse();
        assertThat(aprovacaoRepository.findByArtigoIdIn(Set.of(artigo.getId())))
                .hasSize(1);

        aprovacaoRepository.deleteByArtigoId(artigo.getId());

        assertThat(aprovacaoRepository.countByArtigoId(artigo.getId())).isZero();
    }

    private List<UUID> professores() {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores)
                .as("a V3 semeia professores e a V7 os deixa aprovados")
                .hasSizeGreaterThanOrEqualTo(2);

        return professores.stream().map(UsuarioEntity::getId).toList();
    }

    private ArtigoEntity artigoSalvo(StatusAprovacaoEnum status, UUID autorId, LocalDateTime criadoEm) {
        var artigo = new ArtigoEntity();

        artigo.setTitulo("Artigo de teste de repositorio");
        artigo.setCategoria(CategoriaArtigoEnum.FISIOLOGIA);
        artigo.setResumo("Resumo de teste. ".repeat(20));
        artigo.setFonte("https://doi.org/teste/" + UUID.randomUUID());
        artigo.setStatus(status);
        artigo.setAutorId(autorId);
        artigo.setCriadoEm(criadoEm);

        return artigoRepository.save(artigo);
    }

    private List<UUID> idsDe(List<ArtigoEntity> artigos) {
        return artigos.stream().map(ArtigoEntity::getId).toList();
    }

    private LocalDateTime umaHoraAtras() {
        return LocalDateTime.now().minusHours(1);
    }
}
