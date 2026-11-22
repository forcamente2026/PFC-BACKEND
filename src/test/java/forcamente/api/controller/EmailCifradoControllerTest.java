package forcamente.api.controller;

import forcamente.api.cifra.CifraDeEmail;
import forcamente.api.cifra.CifradorDeEmailsEmClaro;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("E-mail cifrado no banco - quem le a tabela usuarios direto nao ve o endereco")
class EmailCifradoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CifraDeEmail cifraDeEmail;

    @Autowired
    private CifradorDeEmailsEmClaro cifradorDeEmailsEmClaro;

    @MockitoBean
    private IEmailService emailService;

    @Test
    @DisplayName("depois que o back sobe, nenhuma linha da tabela usuarios guarda e-mail em claro nem fica sem indice")
    void naoDeveSobrarEmailEmClaroDepoisDeSubir() {
        Integer emClaro = jdbcTemplate.queryForObject(
                "select count(*) from usuarios where email not like 'v1:%' or email_hash is null", Integer.class);

        assertThat(emClaro).isZero();
    }

    @Test
    @DisplayName("o cadastro grava o e-mail cifrado e o indice; a conta e achada pelo e-mail digitado com maiusculas")
    void deveGravarOEmailCifradoNoCadastro() throws Exception {
        cadastrar("cifra.cadastro@exemplo.com").andExpect(status().isCreated());
        usuarioRepository.flush();

        Map<String, Object> linha = jdbcTemplate.queryForMap(
                "select email, email_hash from usuarios where email_hash = ?",
                cifraDeEmail.indice("cifra.cadastro@exemplo.com"));

        assertThat((String) linha.get("email")).startsWith(CifraDeEmail.PREFIXO).doesNotContain("cifra.cadastro");
        assertThat(usuarioRepository.findByEmail(" Cifra.Cadastro@EXEMPLO.com ")).isPresent()
                .get().extracting(UsuarioEntity::getEmail).isEqualTo("cifra.cadastro@exemplo.com");
    }

    @Test
    @DisplayName("o mesmo e-mail com maiusculas nao abre uma segunda conta: o cadastro responde 409")
    void deveRecusarEmailRepetidoComMaiusculas() throws Exception {
        cadastrar("cifra.repetido@exemplo.com").andExpect(status().isCreated());
        usuarioRepository.flush();

        cadastrar("Cifra.Repetido@Exemplo.com").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("o e-mail pendente da troca tambem vai cifrado para o banco")
    void deveGravarOEmailPendenteCifrado() {
        UsuarioEntity usuario = novaConta("cifra.pendente@exemplo.com");
        usuario.setEmailPendente("cifra.novo@exemplo.com");
        usuarioRepository.saveAndFlush(usuario);

        String pendente = jdbcTemplate.queryForObject(
                "select email_pendente from usuarios where id = ?", String.class, usuario.getId());

        assertThat(pendente).startsWith(CifraDeEmail.PREFIXO).doesNotContain("cifra.novo");
        assertThat(usuarioRepository.findById(usuario.getId())).get()
                .extracting(UsuarioEntity::getEmailPendente).isEqualTo("cifra.novo@exemplo.com");
    }

    @Test
    @DisplayName("trocar o e-mail recalcula o indice: o e-mail antigo deixa de achar a conta e o novo passa a achar")
    void deveRecalcularOIndiceAoTrocarOEmail() {
        UsuarioEntity usuario = novaConta("cifra.antigo@exemplo.com");

        usuario.setEmail("cifra.trocado@exemplo.com");
        usuarioRepository.saveAndFlush(usuario);

        assertThat(usuarioRepository.existsByEmail("cifra.antigo@exemplo.com")).isFalse();
        assertThat(usuarioRepository.findByEmail("cifra.trocado@exemplo.com")).get()
                .extracting(UsuarioEntity::getId).isEqualTo(usuario.getId());
    }

    @Test
    @DisplayName("conta gravada em claro por um script SQL e cifrada pelo back, e o login passa a acha-la")
    void deveCifrarContaGravadaEmClaroPorScript() throws Exception {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into usuarios (id, nome_completo, email, senha_hash, papel, ativo, criado_em)
                values (?, 'Conta de Script', 'cifra.script@exemplo.com', 'x', 'ALUNO', true, now())
                """, id);

        assertThat(usuarioRepository.findByEmail("cifra.script@exemplo.com")).isEmpty();

        cifradorDeEmailsEmClaro.cifrarEmailsEmClaro(DataSourceUtils.getConnection(dataSource));

        String email = jdbcTemplate.queryForObject("select email from usuarios where id = ?", String.class, id);
        assertThat(email).startsWith(CifraDeEmail.PREFIXO);
        assertThat(usuarioRepository.findByEmail("cifra.script@exemplo.com")).get()
                .extracting(UsuarioEntity::getId).isEqualTo(id);
    }

    private ResultActions cadastrar(String email) throws Exception {
        return mockMvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "nomeCompleto": "Conta da Cifra",
                          "email": "%s",
                          "senha": "Senha@Forte123",
                          "dataNascimento": "1998-04-12",
                          "papel": "ALUNO",
                          "aceitouTermosUso": true,
                          "aceitouPoliticaPrivacidade": true,
                          "versaoTermosUso": "1.0",
                          "versaoPoliticaPrivacidade": "1.0"
                        }
                        """.formatted(email)));
    }

    private UsuarioEntity novaConta(String email) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setNomeCompleto("Conta da Cifra");
        usuario.setEmail(email);
        usuario.setSenhaHash("x");
        usuario.setPapel(PapelUsuarioEnum.ALUNO);
        usuario.setAtivo(true);
        usuario.setCriadoEm(LocalDateTime.now());
        return usuarioRepository.saveAndFlush(usuario);
    }
}
