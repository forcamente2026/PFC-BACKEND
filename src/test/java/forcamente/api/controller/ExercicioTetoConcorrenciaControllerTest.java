package forcamente.api.controller;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IExercicioRepository;
import forcamente.api.repository.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Teto de exercicios - criacoes simultaneas do mesmo professor nao passam de tres pendentes")
class ExercicioTetoConcorrenciaControllerTest {

    private static final String PREFIXO = "Concorrencia ";

    private static final int TETO = 3;

    private static final int CRIACOES_SIMULTANEAS = 10;

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IExercicioRepository exercicioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("dez criacoes disparadas ao mesmo tempo param no teto: as vagas livres recebem 201 e o resto 409")
    void deveSegurarOTetoComCriacoesSimultaneas() throws Exception {
        UUID autorId = umProfessorAtivo().getId();
        String prefixo = PREFIXO + UUID.randomUUID();

        apagarCriados(PREFIXO);
        int vagas = TETO - (int) aguardando(autorId);

        assertThat(vagas).as("o professor precisa comecar com vaga livre").isPositive();

        try {
            List<Integer> respostas = dispararAoMesmoTempo(autorId, prefixo);

            assertThat(respostas).filteredOn(status -> status == 201).hasSize(vagas);
            assertThat(respostas).filteredOn(status -> status == 409).hasSize(CRIACOES_SIMULTANEAS - vagas);
            assertThat(aguardando(autorId)).isEqualTo(TETO);
        } finally {
            apagarCriados(prefixo);
        }
    }

    private List<Integer> dispararAoMesmoTempo(UUID autorId, String prefixo) throws Exception {
        HttpClient cliente = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        String token = tokenDeProfessor(autorId);
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(CRIACOES_SIMULTANEAS);

        try {
            List<Future<Integer>> pedidos = new ArrayList<>();

            for (int i = 0; i < CRIACOES_SIMULTANEAS; i++) {
                HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/exercicios"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(corpo(prefixo + " " + i)))
                        .build();

                pedidos.add(executor.submit(() -> {
                    largada.await();
                    return cliente.send(requisicao, HttpResponse.BodyHandlers.discarding()).statusCode();
                }));
            }

            largada.countDown();

            List<Integer> respostas = new ArrayList<>();
            for (Future<Integer> pedido : pedidos) {
                respostas.add(pedido.get(30, TimeUnit.SECONDS));
            }
            return respostas;
        } finally {
            executor.shutdownNow();
        }
    }

    private long aguardando(UUID autorId) {
        return exercicioRepository.countByAutorIdAndStatusNot(autorId, StatusAprovacaoEnum.APROVADO);
    }

    private void apagarCriados(String prefixo) {
        List<UUID> criados = jdbcTemplate.queryForList(
                "select id from exercicios where nome like ?", UUID.class, prefixo + "%");

        for (UUID id : criados) {
            jdbcTemplate.update("delete from registros_auditoria where recurso_id = ?", id);
            jdbcTemplate.update("delete from exercicios where id = ?", id);
        }
    }

    private UsuarioEntity umProfessorAtivo() {
        return usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                        PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO)
                .stream()
                .filter(UsuarioEntity::isAtivo)
                .findFirst()
                .orElseThrow();
    }

    private String corpo(String nome) {
        return """
                {
                  "nome": "%s",
                  "grupoMuscular": "PEITO",
                  "nivel": "INICIANTE",
                  "descricaoExecucao": "Descricao da execucao do exercicio de teste."
                }
                """.formatted(nome);
    }

    private String tokenDeProfessor(UUID usuarioId) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("forcamente-api")
                .subject(usuarioId.toString())
                .claim("papel", "PROFESSOR")
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .build();

        return jwtEncoder.encode(
                        JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
