package forcamente.api.config;

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

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Seguranca - rotas publicas, protegidas e corpo dos 401")
class SegurancaRotasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    @DisplayName("cadastro continua publico: sem token chega ao controller e recebe 400 de validacao")
    void cadastroEhPublico() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos"));
    }

    @Test
    @DisplayName("rota protegida sem token recebe 401 com message")
    void rotaProtegidaSemTokenRecebe401ComCorpo() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("rota protegida com token invalido recebe 401 com message")
    void rotaProtegidaComTokenInvalidoRecebe401ComCorpo() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("rota protegida com token valido recebe 200")
    void rotaProtegidaComTokenValidoRecebe200() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis").header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("formacoes e publica: a tela de cadastro precisa dela antes do login")
    void formacoesEhPublica() throws Exception {
        mockMvc.perform(get("/api/usuarios/formacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("aluno nao pode consultar a trilha de auditoria")
    void alunoNaoPodeConsultarAuditoria() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador pode consultar a trilha de auditoria")
    void administradorPodeConsultarAuditoria() throws Exception {
        mockMvc.perform(get("/api/auditoria")
                        .header("Authorization", "Bearer " + tokenDeTeste("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.pagina").value(0));
    }

    @Test
    @DisplayName("administrador exporta a trilha em CSV, como anexo")
    void administradorExportaCsv() throws Exception {
        mockMvc.perform(get("/api/auditoria/csv")
                        .header("Authorization", "Bearer " + tokenDeTeste("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Quando;Acao;Usuario")));
    }

    @Test
    @DisplayName("o CSV comeca com a BOM do UTF-8, senao o Excel em portugues troca os acentos")
    void csvComecaComBomDeUtf8() throws Exception {
        byte[] corpo = mockMvc.perform(get("/api/auditoria/csv")
                        .header("Authorization", "Bearer " + tokenDeTeste("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(corpo).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
    }

    @Test
    @DisplayName("aluno nao pode exportar a trilha")
    void alunoNaoPodeExportarCsv() throws Exception {
        mockMvc.perform(get("/api/auditoria/csv")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno nao pode listar os usuarios do sistema")
    void alunoNaoPodeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor tambem nao: gestao de usuarios e so do administrador")
    void professorNaoPodeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenDeTeste("PROFESSOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("administrador lista usuarios com o envelope de pagina")
    void administradorPodeListarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenDeTeste("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.totalDePaginas").isNumber());
    }

    @Test
    @DisplayName("a lista de usuarios nunca devolve hash de senha")
    void listaDeUsuariosNaoDevolveSenha() throws Exception {
        String corpo = mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenDeTeste("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("senha");
        assertThat(corpo).doesNotContain("$2a$");
    }

    @Test
    @DisplayName("aluno nao pode editar usuario")
    void alunoNaoPodeEditarUsuario() throws Exception {
        mockMvc.perform(put("/api/usuarios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno nao pode trancar nem destrancar conta de ninguem")
    void alunoNaoPodeAlterarAtivo() throws Exception {
        mockMvc.perform(patch("/api/usuarios/" + UUID.randomUUID() + "/ativo")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno nao le os dados de outra conta: 403 antes de chegar ao banco")
    void alunoNaoLeOutraConta() throws Exception {
        mockMvc.perform(get("/api/usuarios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    private String tokenDeTeste(String papel) {
        var agora = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("forcamente-api")
                .subject(UUID.randomUUID().toString())
                .claim("papel", papel)
                .issuedAt(agora)
                .expiresAt(agora.plusSeconds(300))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    @DisplayName("aluno pode listar exercicios")
    void alunoPodeListarExercicios() throws Exception {
        mockMvc.perform(get("/api/exercicios").header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("aluno nao pode criar exercicio: 403 com message, antes de qualquer validacao")
    void alunoNaoPodeCriarExercicio() throws Exception {
        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    @Test
    @DisplayName("aluno nao pode excluir exercicio, mesmo inexistente: 403 vem antes do 404")
    void alunoNaoPodeExcluirExercicio() throws Exception {
        mockMvc.perform(delete("/api/exercicios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("professor passa pela autorizacao ao criar exercicio: chega ao controller e recebe 400 de validacao")
    void professorPassaPelaAutorizacaoAoCriar() throws Exception {
        mockMvc.perform(post("/api/exercicios")
                        .header("Authorization", "Bearer " + tokenDeTeste("PROFESSOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados invalidos"));
    }

    @Test
    @DisplayName("documentos legais sao publicos: sem token devolve os dois vigentes")
    void documentosLegaisSaoPublicos() throws Exception {
        mockMvc.perform(get("/api/documentos-legais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versao").isNotEmpty());
    }
    @Test
    @DisplayName("esqueci-senha e publica e responde 200 mesmo para e-mail inexistente")
    void esqueciSenhaEhPublica() throws Exception {
        mockMvc.perform(post("/api/auth/esqueci-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem-mesmo@umc.br\"}"))
                .andExpect(status().isOk());
    }
    @Test
    @DisplayName("login/verificar e publica e responde 401 para e-mail inexistente")
    void loginVerificarEhPublica() throws Exception {
        mockMvc.perform(post("/api/auth/login/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem-mesmo@umc.br\",\"codigo\":\"1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Codigo invalido ou expirado"));
    }
    @Test
    @DisplayName("enderecos e publica: CEP mal formado da 400, nao 401")
    void enderecosEhPublica() throws Exception {
        mockMvc.perform(get("/api/enderecos/123"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("aluno nao pode listar anonimizacoes pendentes")
    void alunoNaoPodeListarAnonimizacoes() throws Exception {
        mockMvc.perform(get("/api/usuarios/anonimizacoes-pendentes")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("aluno nao pode anonimizar outra conta")
    void alunoNaoPodeAnonimizarOutraConta() throws Exception {
        mockMvc.perform(post("/api/usuarios/" + UUID.randomUUID() + "/anonimizar")
                        .header("Authorization", "Bearer " + tokenDeTeste("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("solicitar a propria anonimizacao exige apenas estar logado")
    void solicitarAnonimizacaoExigeLogin() throws Exception {
        mockMvc.perform(post("/api/usuarios/me/anonimizacao"))
                .andExpect(status().isUnauthorized());
    }

}