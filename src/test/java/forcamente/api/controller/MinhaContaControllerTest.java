package forcamente.api.controller;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.AcaoAuditoriaEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IRegistroAuditoriaRepository;
import forcamente.api.repository.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Minha conta - correcao dos proprios dados")
class MinhaContaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRegistroAuditoriaRepository registroAuditoriaRepository;

    @Test
    @DisplayName("o aluno corrige o proprio nome: a regra do administrador em PUT /api/usuarios/* nao o barra")
    void deveDeixarOAlunoCorrigirOProprioNome() throws Exception {
        var aluno = umAluno();

        mockMvc.perform(put("/api/usuarios/me")
                        .header("Authorization", "Bearer " + tokenPara(aluno.getId(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"Ana Ramos de Souza\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("Ana Ramos de Souza"));
    }

    @Test
    @DisplayName("o aluno mandando endereco recebe 400: endereco e so do professor")
    void deveRecusarEnderecoDeQuemNaoEProfessor() throws Exception {
        var aluno = umAluno();

        mockMvc.perform(put("/api/usuarios/me")
                        .header("Authorization", "Bearer " + tokenPara(aluno.getId(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeCompleto": "Ana Ramos",
                                  "cep": "08780000",
                                  "logradouro": "Rua das Flores",
                                  "numero": "100",
                                  "bairro": "Centro",
                                  "cidade": "Mogi das Cruzes",
                                  "estado": "SP"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o professor sem endereco recebe 400, menos o complemento")
    void deveExigirEnderecoDoProfessor() throws Exception {
        var professor = umProfessor();

        mockMvc.perform(put("/api/usuarios/me")
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"Carlos Andrade\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o professor corrige nome e endereco, e a sigla do estado sai em maiusculas")
    void deveCorrigirNomeEEnderecoDoProfessor() throws Exception {
        var professor = umProfessor();

        mockMvc.perform(put("/api/usuarios/me")
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeCompleto": "  Carlos Andrade Filho  ",
                                  "cep": "08780000",
                                  "logradouro": "Rua das Flores",
                                  "numero": "100",
                                  "complemento": "",
                                  "bairro": "Centro",
                                  "cidade": "Mogi das Cruzes",
                                  "estado": "sp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("Carlos Andrade Filho"))
                .andExpect(jsonPath("$.cidade").value("Mogi das Cruzes"))
                .andExpect(jsonPath("$.estado").value("SP"));
    }

    @Test
    @DisplayName("nome em branco responde 400 com a lista de campos, e marcacao no nome tambem")
    void deveValidarONome() throws Exception {
        var aluno = umAluno();
        var token = tokenPara(aluno.getId(), "ALUNO");

        mockMvc.perform(put("/api/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isNotEmpty());

        mockMvc.perform(put("/api/usuarios/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo",
                        org.hamcrest.Matchers.hasItem("nomeCompleto")));
    }

    @Test
    @DisplayName("sem token, corrigir a propria conta responde 401")
    void deveResponder401SemToken() throws Exception {
        mockMvc.perform(put("/api/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeCompleto\":\"Alguem\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("com a verificacao rejeitada o professor corrige o proprio CREF e volta para a fila")
    void deveDeixarOProfessorRejeitadoCorrigirOCref() throws Exception {
        var professor = umProfessor();

        professor.setSituacaoCref(StatusAprovacaoEnum.REJEITADO);
        professor.setMotivoRejeicaoCref("O numero nao confere com o registro");
        usuarioRepository.saveAndFlush(professor);

        mockMvc.perform(patch("/api/usuarios/me/cref")
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cref\":\"654321-G/SP\",\"formacao\":\"BACHARELADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cref").value("654321-G/SP"))
                .andExpect(jsonPath("$.situacaoCref").value("PENDENTE"))
                .andExpect(jsonPath("$.motivoRejeicaoCref").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("com a verificacao pendente a correcao do CREF continua recusada")
    void deveRecusarCorrecaoComVerificacaoPendente() throws Exception {
        var professor = umProfessor();

        professor.setSituacaoCref(StatusAprovacaoEnum.PENDENTE);
        usuarioRepository.saveAndFlush(professor);

        mockMvc.perform(patch("/api/usuarios/me/cref")
                        .header("Authorization", "Bearer " + tokenPara(professor.getId(), "PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cref\":\"654321-G/SP\",\"formacao\":\"BACHARELADO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("com a senha atual certa a troca responde 204")
    void deveTrocarASenha() throws Exception {
        var aluno = umAluno();

        mockMvc.perform(patch("/api/usuarios/me/senha")
                        .header("Authorization", "Bearer " + tokenPara(aluno.getId(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"Senha@123\",\"novaSenha\":\"Senha@456\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("senha atual errada responde 400, nunca 401: o front trata 401 como sessao vencida")
    void deveResponder400ENao401ComASenhaAtualErrada() throws Exception {
        var aluno = umAluno();

        mockMvc.perform(patch("/api/usuarios/me/senha")
                        .header("Authorization", "Bearer " + tokenPara(aluno.getId(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"Errada@123\",\"novaSenha\":\"Senha@456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("A senha atual nao confere"));
    }

    @Test
    @DisplayName("a falha de senha atual entra na trilha como SENHA_FALHOU, pela decisao de 31/10")
    void deveRegistrarAFalhaNaTrilha() throws Exception {
        var aluno = umAluno();
        var antes = LocalDateTime.now().minusMinutes(1);

        mockMvc.perform(patch("/api/usuarios/me/senha")
                        .header("Authorization", "Bearer " + tokenPara(aluno.getId(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"Errada@123\",\"novaSenha\":\"Senha@456\"}"))
                .andExpect(status().isBadRequest());

        var registros = registroAuditoriaRepository
                .findByOcorridoEmBetweenAndAcaoOrderByOcorridoEmDesc(
                        antes, LocalDateTime.now().plusMinutes(1), AcaoAuditoriaEnum.SENHA_FALHOU);

        assertThat(registros)
                .as("a trilha e a unica defesa escolhida, entao a linha precisa sobreviver ao 400")
                .isNotEmpty();
        assertThat(registros.getFirst().getRecursoId()).isEqualTo(aluno.getId());
    }

    @Test
    @DisplayName("nova senha fora da politica responde 400 com a lista de campos")
    void deveExigirAPoliticaNaNovaSenha() throws Exception {
        var aluno = umAluno();

        mockMvc.perform(patch("/api/usuarios/me/senha")
                        .header("Authorization", "Bearer " + tokenPara(aluno.getId(), "ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"Senha@123\",\"novaSenha\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[*].campo",
                        org.hamcrest.Matchers.hasItem("novaSenha")));
    }

    @Test
    @DisplayName("sem token, trocar a senha responde 401")
    void deveResponder401NaTrocaDeSenhaSemToken() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me/senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"Senha@123\",\"novaSenha\":\"Senha@456\"}"))
                .andExpect(status().isUnauthorized());
    }

    private UsuarioEntity umAluno() {
        var aluno = usuarioRepository.findByEmail("ana.ramos@exemplo.test");

        assertThat(aluno).as("a V3 semeia esta conta").isPresent();

        return aluno.get();
    }

    private UsuarioEntity umProfessor() {
        var professores = usuarioRepository.findByPapelAndSituacaoCrefOrderByCriadoEm(
                PapelUsuarioEnum.PROFESSOR, StatusAprovacaoEnum.APROVADO);

        assertThat(professores).as("a V3 semeia professores e a V7 os aprova").isNotEmpty();

        return professores.getFirst();
    }

    private String tokenPara(UUID usuarioId, String papel) {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("forcamente-api")
                .subject(usuarioId.toString())
                .claim("papel", papel)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .build();

        return jwtEncoder.encode(
                        JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
